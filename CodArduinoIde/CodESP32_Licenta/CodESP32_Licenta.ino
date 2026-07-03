#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

//-------------------- Ultrasonic --------------------
const int trig = 19;
const int echo = 18;

//-------------------- Servo --------------------
const int servoPin = 13;
const int servoFreq = 50;
const int servoRes = 16;

//-------------------- Motor Pins (4x4) --------------------
const int RL_IN1 = 2;
const int RL_IN2 = 16;
const int RL_EN  = 4;
const int RR_IN1 = 17;
const int RR_IN2 = 5;
const int RR_EN  = 23;
const int FL_IN1 = 26;
const int FL_IN2 = 25;
const int FL_EN  = 27;
const int FR_IN1 = 32;
const int FR_IN2 = 33;
const int FR_EN  = 14;

//-------------------- Motor Speeds --------------------
const int MOTOR_SPEED_MANUAL = 160;
const int MOTOR_SPEED_AUTO   = 150;
const int AVOID_SPEED        = 160;

//-------------------- Timing --------------------
const int TURN_45_TIME       = 263;     //calibrate for your surface (10 turns should be 450°)
const int BACKUP_TIME        = 300;

//-------------------- Motor compensation --------------------
int LEFT_COMP  = 110;
int RIGHT_COMP = 95;

//-------------------- Robot Position --------------------
float X = 0;
float Y = 0;
float theta = 0;   //heading in degrees (0 = north)

//-------------------- Obstacle Map (circular buffer) --------------------
#define MAX_OBS 200
struct Point { float x; float y; };
Point obstacles[MAX_OBS];
int obsIndex = 0;
int obsCount = 0;

//-------------------- State --------------------
bool autoMode = false;

enum AutoState {
  DRIVE,
  SCAN,
  TURN,
  FOLLOW_EDGE,
  RETURN,
  BACKUP
};
AutoState autoState = DRIVE;

enum AvoidDir { NONE, RIGHT, LEFT };
AvoidDir avoidDir = NONE;

unsigned long lastHeartbeat = 0;
unsigned long lastManualScan = 0;
int currentServoAngle = 90;

//Side scan angles – adjust based on your physical servo orientation
const int LEFT_SIDE  = 150;   //change to 30 if left/right are swapped
const int RIGHT_SIDE = 30;
const int LEFT_MID   = 60;
const int RIGHT_MID  = 120;

//-------------------- Servo Helper --------------------
int angleToDuty(int angle) {
  int minUs = 500;
  int maxUs = 2400;
  int us = map(angle, 0, 180, minUs, maxUs);
  return (us * 65535) / 20000;
}

void setServoAngle(int angle) {
  if (angle < 0) angle = 0;
  if (angle > 180) angle = 180;
  currentServoAngle = angle;
  ledcWrite(servoPin, angleToDuty(angle));
  delay(50);
}

void centerSensor() {
  setServoAngle(90);
  delay(100);
}

//-------------------- Communication --------------------
void sendPosition() {
  SerialBT.print("POS:");
  SerialBT.print(X, 1);
  SerialBT.print(",");
  SerialBT.print(Y, 1);
  SerialBT.print(",");
  SerialBT.println(theta, 1);
}

//-------------------- Ultrasonic --------------------
long getDistanceRaw() {
  digitalWrite(trig, LOW);
  delayMicroseconds(3);
  digitalWrite(trig, HIGH);
  delayMicroseconds(12);
  digitalWrite(trig, LOW);

  long duration = pulseIn(echo, HIGH, 20000);
  if (duration <= 0) return 300;

  long dist = duration / 60;
  if (dist < 2) dist = 2;
  if (dist > 250) dist = 250;
  return dist;
}

long getDistance() {
  long vals[5];
  for (int i = 0; i < 5; i++) {
    vals[i] = getDistanceRaw();
    delay(5);
  }
  // median filter
  for (int i = 0; i < 5; i++) {
    for (int j = i + 1; j < 5; j++) {
      if (vals[j] < vals[i]) {
        long tmp = vals[i];
        vals[i] = vals[j];
        vals[j] = tmp;
      }
    }
  }
  return vals[2];
}

//-------------------- Quick Scan (world coordinates) --------------------
long quickScan(int angle) {
  setServoAngle(angle);
  delay(200);
  long d = getDistance();
  delay(20);

  SerialBT.print("RADAR:");
  SerialBT.print(angle);
  SerialBT.print(",");
  SerialBT.println(d);

  //Convert local sensor reading to global world coordinates
  //angle=90 → straight ahead (0° offset)
  //angle=0  → left (90° offset)
  //angle=180→ right (-90° offset)
  float sensorOffsetDeg = angle - 90;
  float worldAngleDeg = theta + sensorOffsetDeg;
  float worldAngleRad = worldAngleDeg * 0.0174533;

  float globalX = X + d * sin(worldAngleRad);
  float globalY = Y + d * cos(worldAngleRad);

  if (d < 250) {
    //Circular buffer – overwrite oldest
    obstacles[obsIndex] = {globalX, globalY};
    obsIndex = (obsIndex + 1) % MAX_OBS;
    if (obsCount < MAX_OBS) obsCount++;

    SerialBT.print("OBS:");
    SerialBT.print(globalX, 1);
    SerialBT.print(",");
    SerialBT.println(globalY, 1);
  }

  return d;
}

//-------------------- Motor Control --------------------
void stopCar() {
  digitalWrite(FL_IN1, LOW); digitalWrite(FL_IN2, LOW);
  digitalWrite(FR_IN1, LOW); digitalWrite(FR_IN2, LOW);
  digitalWrite(RL_IN1, LOW); digitalWrite(RL_IN2, LOW);
  digitalWrite(RR_IN1, LOW); digitalWrite(RR_IN2, LOW);
  ledcWrite(FL_EN, 0); ledcWrite(FR_EN, 0);
  ledcWrite(RL_EN, 0); ledcWrite(RR_EN, 0);
}

void moveForward(int speed) {
  digitalWrite(FL_IN1, HIGH); digitalWrite(FL_IN2, LOW);
  digitalWrite(FR_IN1, LOW);  digitalWrite(FR_IN2, HIGH);
  digitalWrite(RL_IN1, LOW);  digitalWrite(RL_IN2, HIGH);
  digitalWrite(RR_IN1, LOW);  digitalWrite(RR_IN2, HIGH);

  float left  = speed * LEFT_COMP  / 100.0;
  float right = speed * RIGHT_COMP / 100.0;

  ledcWrite(FL_EN, left);  ledcWrite(FR_EN, right);
  ledcWrite(RL_EN, left);  ledcWrite(RR_EN, right);
}

void moveBackward(int speed) {
  digitalWrite(FL_IN1, LOW);  digitalWrite(FL_IN2, HIGH);
  digitalWrite(FR_IN1, HIGH); digitalWrite(FR_IN2, LOW);
  digitalWrite(RL_IN1, HIGH); digitalWrite(RL_IN2, LOW);
  digitalWrite(RR_IN1, HIGH); digitalWrite(RR_IN2, LOW);

  float left  = speed * LEFT_COMP  / 100.0;
  float right = speed * RIGHT_COMP / 100.0;

  ledcWrite(FL_EN, left);  ledcWrite(FR_EN, right);
  ledcWrite(RL_EN, left);  ledcWrite(RR_EN, right);
}

void turnLeftMotors(int speed) {
  digitalWrite(FL_IN1, LOW);  digitalWrite(FL_IN2, HIGH);
  digitalWrite(RL_IN1, LOW);  digitalWrite(RL_IN2, HIGH);
  digitalWrite(FR_IN1, LOW);  digitalWrite(FR_IN2, HIGH);
  digitalWrite(RR_IN1, HIGH); digitalWrite(RR_IN2, LOW);

  ledcWrite(FL_EN, speed);
  ledcWrite(FR_EN, speed);
  ledcWrite(RL_EN, speed);
  ledcWrite(RR_EN, speed);
}

void turnRightMotors(int speed) {
  digitalWrite(FR_IN1, HIGH); digitalWrite(FR_IN2, LOW);
  digitalWrite(RR_IN1, LOW);  digitalWrite(RR_IN2, HIGH);
  digitalWrite(FL_IN1, HIGH); digitalWrite(FL_IN2, LOW);
  digitalWrite(RL_IN1, HIGH); digitalWrite(RL_IN2, LOW);

  ledcWrite(FL_EN, speed);
  ledcWrite(FR_EN, speed);
  ledcWrite(RL_EN, speed);
  ledcWrite(RR_EN, speed);
}

//-------------------- TURN 45° FUNCTIONS --------------------
void executeTurnLeft45() {
  SerialBT.println("TURN:LEFT_45");
  turnLeftMotors(AVOID_SPEED);
  delay(TURN_45_TIME);
  stopCar();
  delay(100);
  theta -= 45;
  if (theta < 0) theta += 360;
  sendPosition();
}

void executeTurnRight45() {
  SerialBT.println("TURN:RIGHT_45");
  turnRightMotors(AVOID_SPEED);
  delay(TURN_45_TIME);
  stopCar();
  delay(100);
  theta += 45;
  if (theta >= 360) theta -= 360;
  sendPosition();
}

//-------------------- AUTO MODE --------------------
void runAutoMode() {
  switch (autoState) {

    case DRIVE: {
      moveForward(MOTOR_SPEED_AUTO);
      delay(120);
      stopCar();
      centerSensor();

      float rad = theta * 0.0174533;
      X += 6.7 * sin(rad);
      Y += 6.7 * cos(rad);

      if (getDistance() < 30) {
        SerialBT.println("OBSTACLE_AHEAD");
        autoState = SCAN;
      }
      break;
    }

    case SCAN: {
      SerialBT.println("SCANNING_SIDES");

      long distLeftFar  = quickScan(LEFT_SIDE);
      long distLeftMid  = quickScan(LEFT_MID);
      long distRightMid = quickScan(RIGHT_MID);
      long distRightFar = quickScan(RIGHT_SIDE);
      centerSensor();

      long leftSpace  = min(distLeftFar, distLeftMid);
      long rightSpace = min(distRightMid, distRightFar);

      if (leftSpace < 25 && rightSpace < 25) {
        SerialBT.println("BOTH_BLOCKED");
        autoState = BACKUP;
      }
      else if (leftSpace > rightSpace + 15) {
        SerialBT.println("GO_LEFT");
        avoidDir = LEFT;
        autoState = TURN;
      }
      else {
        SerialBT.println("GO_RIGHT");
        avoidDir = RIGHT;
        autoState = TURN;
      }
      break;
    }

    case TURN: {
      if (avoidDir == LEFT) {
        executeTurnLeft45();
      } else {
        executeTurnRight45();
      }

      centerSensor();
      delay(100);
      long frontClear = getDistance();
      long sideClear;
      if (avoidDir == RIGHT) {
        sideClear = quickScan(LEFT_SIDE);
      } else {
        sideClear = quickScan(RIGHT_SIDE);
      }
      centerSensor();

      if (frontClear < 25 || sideClear < 15) {
        SerialBT.println("TURNED_INTO_DANGER");
        autoState = BACKUP;
      } else {
        autoState = FOLLOW_EDGE;
      }
      break;
    }

    case FOLLOW_EDGE: {
      moveForward(MOTOR_SPEED_AUTO);
      delay(150);
      stopCar();

      float rad = theta * 0.0174533;
      X += 8.4 * sin(rad);
      Y += 8.4 * cos(rad);
      centerSensor();

      if (getDistance() < 25) {
        SerialBT.println("NEW_OBSTACLE");
        autoState = BACKUP;
        break;
      }

      long sideDist;
      if (avoidDir == RIGHT) {
        sideDist = quickScan(LEFT_SIDE);
      } else {
        sideDist = quickScan(RIGHT_SIDE);
      }
      centerSensor();

      int clearThreshold = (avoidDir == LEFT) ? 40 : 55;

      if (sideDist < 20) {
        SerialBT.println("STEER_AWAY");
        if (avoidDir == RIGHT) {
          turnRightMotors(AVOID_SPEED);
          delay(100);
          stopCar();
        } else {
          turnLeftMotors(AVOID_SPEED);
          delay(100);
          stopCar();
        }
      }

      if (sideDist > clearThreshold) {
        SerialBT.println("OBSTACLE_GONE");
        autoState = RETURN;
      }
      break;
    }

    case RETURN: {
      if (avoidDir == RIGHT) {
        executeTurnLeft45();
      } else {
        executeTurnRight45();
      }
      avoidDir = NONE;

      centerSensor();
      delay(100);
      if (getDistance() < 30) {
        autoState = SCAN;
      } else {
        autoState = DRIVE;
      }
      break;
    }

    case BACKUP: {
      SerialBT.println("BACKING_UP");
      moveBackward(AVOID_SPEED);
      delay(300);
      stopCar();

      float rad = theta * 0.0174533;
      X -= 16.8 * sin(rad);
      Y -= 16.8 * cos(rad);

      autoState = SCAN;
      break;
    }
  }
}

//-------------------- Setup --------------------
void setup() {
  Serial.begin(115200);
  delay(500);
  SerialBT.begin("ESP32-4x4-Car");

  stopCar();

  pinMode(trig, OUTPUT);
  pinMode(echo, INPUT);

  pinMode(FL_IN1, OUTPUT); pinMode(FL_IN2, OUTPUT);
  pinMode(FR_IN1, OUTPUT); pinMode(FR_IN2, OUTPUT);
  pinMode(RL_IN1, OUTPUT); pinMode(RL_IN2, OUTPUT);
  pinMode(RR_IN1, OUTPUT); pinMode(RR_IN2, OUTPUT);

  ledcAttach(FL_EN, 1000, 8);
  ledcAttach(FR_EN, 1000, 8);
  ledcAttach(RL_EN, 1000, 8);
  ledcAttach(RR_EN, 1000, 8);
  ledcAttach(servoPin, servoFreq, servoRes);

  delay(500);
  centerSensor();

  sendPosition();
  SerialBT.println("READY");
}

//-------------------- Loop --------------------
void loop() {
  if (millis() - lastHeartbeat > 1000) {
    lastHeartbeat = millis();
    SerialBT.println("PING");
  }

  if (SerialBT.available()) {
    char c = SerialBT.read();

    if (c == 'A') {
      autoMode = true;
      autoState = DRIVE;
      avoidDir = NONE;
      stopCar();
      centerSensor();

      //Reset map and position on auto start
      X = 0;
      Y = 0;
      theta = 0;
      obsIndex = 0;
      obsCount = 0;
      sendPosition();

      SerialBT.println("AUTO_MODE_ON");
    }

    if (c == 'M') {
      autoMode = false;
      stopCar();
      centerSensor();

      //Clear map in manual mode too
      obsIndex = 0;
      obsCount = 0;

      SerialBT.println("MANUAL_MODE");
    }

    if (!autoMode) {
      if (c == 'F') {
        moveForward(MOTOR_SPEED_MANUAL);
        delay(357);   //calibrated for 20 cm
        stopCar();
        float rad = theta * 0.0174533;
        X += 20 * sin(rad);
        Y += 20 * cos(rad);
        sendPosition();
      }
      if (c == 'B') {
        moveBackward(MOTOR_SPEED_MANUAL);
        delay(357);
        stopCar();
        float rad = theta * 0.0174533;
        X -= 20 * sin(rad);
        Y -= 20 * cos(rad);
        sendPosition();
      }
      if (c == 'L') {
        executeTurnLeft45();
      }
      if (c == 'R') {
        executeTurnRight45();
      }
      if (c == 'S') {
        stopCar();
      }
    }
  }

  if (autoMode) {
    runAutoMode();
    delay(50);
    return;
  }

  //Manual mode – send front radar + obstacle positions
  if (!autoMode && millis() - lastManualScan > 200) {
    lastManualScan = millis();
    if (currentServoAngle != 90) centerSensor();

    long d = getDistance();
    SerialBT.print("RADAR:90,");
    SerialBT.println(d);

    float rad = theta * 0.0174533;
    float globalX = X + d * sin(rad);
    float globalY = Y + d * cos(rad);
    if (d < 250) {
      obstacles[obsIndex] = {globalX, globalY};
      obsIndex = (obsIndex + 1) % MAX_OBS;
      if (obsCount < MAX_OBS) obsCount++;

      SerialBT.print("OBS:");
      SerialBT.print(globalX, 1);
      SerialBT.print(",");
      SerialBT.println(globalY, 1);
    }
  }

  delay(30);
}