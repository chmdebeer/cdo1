#include <AndroidAccessory.h>
#include <math.h>
#include <MotorShield.h>

MS_DCMotor motorL(MOTOR_A);
MS_DCMotor motorR(MOTOR_B);

// accessory descriptor. It's how Arduino identifies itself to Android
char accessoryName[] = "cdo1"; // your Arduino board
char companyName[] = "chmdebeer";
char description[] = "Testing";
char ver[] = "0.1";
char uri[] = "https://www.chmdebeer.ca";
char serial[] = "1234";

const int pingPinFront = 22;
const int pingPinFrontLeft = 26;
const int pingPinFrontRight = 24;

long spaceFront;
long spaceFrontLeft;
long spaceFrontRight;

uint32_t timer;
uint32_t watchdog;

// initialize the accessory:
AndroidAccessory usb(companyName, accessoryName, description, ver);

void setup() {
  motorL.setSpeed(0);
  motorR.setSpeed(0);

  Serial.begin( 115200 );
  usb.begin();
}

void loop() {
  uint8_t msg[64] = { 0x00 };
  const char* recv = "Received: "; 

  spaceFront = measureDistance(pingPinFront);
  spaceFrontLeft = measureDistance(pingPinFrontLeft);
  spaceFrontRight = measureDistance(pingPinFrontRight);
   
//   accessory.refresh();
  if (usb.isConnected()) { // isConnected makes sure the USB connection is ope
   
    uint16_t len = 0;
    uint16_t power = 0;
    uint16_t angle = 0;
    
    while ((usb.available() > 0) && (len < 64)) {
      msg[len] = usb.read();
      len++;
    }

    if ((len > 3) && (msg[0] == 0x02)) {
      Serial.print("\r\n");
      power = msg[1];
      angle = msg[3];
      angle = angle << 8;
      angle = angle | msg[2];
      Serial.print(power, DEC);
      Serial.print(", ");
      Serial.print(angle, DEC);
      Serial.print(", ");
      Serial.print(spaceFrontLeft, DEC);
      Serial.print(", ");
      Serial.print(spaceFront, DEC);
      Serial.print(", ");
      Serial.print(spaceFrontRight, DEC);
      setMotor(angle, power);
    }
  }
}

void setMotor(int angle, int power) {
  float rad;
  float left;
  float right;
  uint8_t motorLeft;
  uint8_t motorRight;
  
  rad = (angle/180.0)*3.14159265359;
  if (angle == 90) {
    left = -1.0;
  } else if (angle >= 270) {
    left = 1.0;
  } else if (angle >= 180) {
    left = -1.0;
  } else {
    left = cos(rad);
  }

  if (angle <= 90) {
    right = 1.0;
  } else if (angle <= 180) {
    right = -1.0;
  } else if (angle == 270) {
    right = -1.0;
  } else {
    right = cos(rad);
  }

  motorLeft = (uint8_t)(left * power) & 0xFF;
  motorRight = (uint8_t)(right * power) & 0xFF;
  
  Serial.print("\r\n");
  Serial.print(motorLeft, DEC);
  Serial.print(", ");
  Serial.print(motorRight, DEC);

  //motorL.run(BRAKE);
  //motorL.setSpeed(255);
  //motorR.run(BRAKE);
  //motorR.setSpeed(255);
  //motorR.run(FORWARD|RELEASE);
  //motorR.setSpeed(0);
  motorL.setSpeed(motorLeft);
  motorR.setSpeed(motorRight);
  if (left < 0) {
    motorL.run(BACKWARD|RELEASE);
  } else {
    motorL.run(FORWARD|RELEASE);
  }

  if (right < 0) {
    motorR.run(BACKWARD|RELEASE);
  } else {
    motorR.run(FORWARD|RELEASE);
  }
  
}

// helper function to print the motor's states in human-readable strings.
//   Serial.print(decodeState(motor.getState()));
String decodeState(int state) {
  String result = "";
  switch (state) {
    case FORWARD:
      result = "Forward";
      break;
    case BACKWARD:
      result = "Backward";
      break;
    case BRAKE:
     result = "Brake";
     break;
   case RELEASE:
     result = "Release";
     break;
   }
  return result;
}

long measureDistance(int pingPin) {
  // establish variables for duration of the ping,
  // and the distance result in inches and centimeters:
  long duration, cm;

  // The PING))) is triggered by a HIGH pulse of 2 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  pinMode(pingPin, OUTPUT);
  digitalWrite(pingPin, LOW);
  delayMicroseconds(2);
  digitalWrite(pingPin, HIGH);
  delayMicroseconds(5);
  digitalWrite(pingPin, LOW);

  // The same pin is used to read the signal from the PING))): a HIGH
  // pulse whose duration is the time (in microseconds) from the sending
  // of the ping to the reception of its echo off of an object.
  pinMode(pingPin, INPUT);
  duration = pulseIn(pingPin, HIGH);

  // convert the time into a distance
  cm = microsecondsToCentimeters(duration);
  return cm;
  
}

long microsecondsToCentimeters(long microseconds) {
  // The speed of sound is 340 m/s or 29 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  return microseconds / 29 / 2;
}










