#include <adk.h>
#include <math.h>
#include <MotorShield.h>

MS_DCMotor motorL(MOTOR_A);
MS_DCMotor motorR(MOTOR_B);

USB Usb;
ADK adk(&Usb, "chmdebeer", // Manufacturer Name
              "cdo1", // Model Name
              "Example sketch for the USB Host Shield", // Description (user-visible string)
              "0.1", // Version
              "http://www.tkjelectronics.dk/uploads/ArduinoBlinkLED.apk", // URL (web page to visit if no installed apps support the accessory)
              "123456789"); // Serial Number (optional)

#define LED LED_BUILTIN // Use built in LED  - note that pin 13 is occupied by the SCK pin on a normal Arduino (Uno, Duemilanove etc.), so use a different pin

const int pingPinFront = 22;
const int pingPinFrontLeft = 26;
const int pingPinFrontRight = 24;

long spaceFront;
long spaceFrontLeft;
long spaceFrontRight;

uint32_t sendTimer;
uint32_t watchdog;


boolean connected;



void setup() {
  Serial.begin(115200);
  while (!Serial); // Wait for serial port to connect - used on Leonardo, Teensy and other boards with built-in USB CDC serial connection
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }
  pinMode(LED, OUTPUT);
  Serial.print("\r\nArduino Blink LED Started");

    // engage the motor's brake 
  //motorL.run(BRAKE);
  //motorL.setSpeed(255);
  //motorR.run(BRAKE);
  //motorR.setSpeed(255);
  //motorR.run(FORWARD|RELEASE);
  //motorR.setSpeed(0);

}

void loop() {

  spaceFront = measureDistance(pingPinFront);
  spaceFrontLeft = measureDistance(pingPinFrontLeft);
  spaceFrontRight = measureDistance(pingPinFrontRight);
  
  Usb.Task();

  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      Serial.print(F("\r\nConnected to accessory"));
    }

    uint8_t msg[4];
    uint16_t len = sizeof(msg);
    uint8_t rcode = adk.RcvData(&len, msg);

    if (rcode && rcode != hrNAK) {
      Serial.print(F("\r\nData rcv: "));
      Serial.print(rcode, HEX);
    } else if (len > 0) {
      Serial.print(F("\r\nData Packet: "));
      Serial.print(msg[0]);
      Serial.print(F("\r\nRed: "));
      Serial.print(msg[1], HEX);
      uint8_t r = (msg[1]) & 0xFF;
      uint8_t g = (msg[2]) & 0xFF;
      uint8_t b = (msg[3]) & 0xFF;
      r = r<<1;
      r = r + 55;
      Serial.print(F("\r\nMotor: "));
      Serial.print(r, HEX);
//      motorR.setSpeed(r);
      analogWrite(LED, r);
      //analogWrite(LED1_GREEN, 255 - g);
      //analogWrite(LED1_BLUE, 255 - b);
      //digitalWrite(LED, msg[0] ? HIGH : LOW);
    }

    watchdog = millis();
    
    if (millis() - timer >= 500) { // Send data every 1s
      timer = millis();
      rcode = adk.SndData(sizeof(timer), (uint8_t*)&timer);
      if (rcode && rcode != hrNAK) {
        Serial.print(F("\r\nData send: "));
        Serial.print(rcode, HEX);
      } else if (rcode != hrNAK) {
        Serial.print(F("\r\nTimer: "));
        Serial.print(timer);
      }
    }
  } else {
    if (connected) {
      connected = false;
      Serial.print(F("\r\nDisconnected from accessory"));
      digitalWrite(LED, LOW);
    }
  }
}

void setMotor(int angle, int power) {
  float rad;
  float left;
  float right;
  
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

