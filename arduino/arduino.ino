#include <adk.h>
#include <MotorShield.h>

MS_DCMotor motorL(MOTOR_A);
MS_DCMotor motorR(MOTOR_B);
USB Usb;
ADK adk(&Usb, "chmdebeer", // Manufacturer Name
  "cdo1", // Model Name
  "CDO1 controller", // Description (user-visible string)
  "0.1", // Version
  "http://www.chmdebeer.com/uploads/cdo1android.apk", // URL (web page to visit if no installed apps support the accessory)
  "20141225"); // Serial Number (optional)

#define  LED1_RED       5
#define  LED1_GREEN       4
#define  LED1_BLUE       3
#define LED LED_BUILTIN // Use built in LED - note that pin 13 is occupied by the SCK pin on a normal Arduino (Uno, Duemilanove etc.), so use a different pin
uint32_t timer;
boolean connected;

void init_leds()
{
    //digitalWrite(LED1_RED, 0);
    //digitalWrite(LED1_GREEN, 0);
    //digitalWrite(LED1_BLUE, 0);

    //pinMode(LED1_RED, OUTPUT);
    //pinMode(LED1_GREEN, OUTPUT);
    //pinMode(LED1_BLUE, OUTPUT);
}

void setup() {
  connected = false;
  Serial.begin(9600);
  //Serial.begin(115200);
  while (!Serial); // Wait for serial port to connect - used on Leonardo, Teensy and other boards with built-in USB CDC serial connection
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }

  init_leds();
  pinMode(LED, OUTPUT);
  Serial.print("\r\nArduino Blink LED Started");

  // engage the motor's brake 
  //motorL.run(BRAKE);
  //motorL.setSpeed(255);
  //motorR.run(BRAKE);
  //motorR.setSpeed(255);
  motorR.run(FORWARD|RELEASE);
  motorR.setSpeed(0);
}

void loop() {
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
      motorR.setSpeed(r);
      //analogWrite(LED1_RED, 255 - r);
      //analogWrite(LED1_GREEN, 255 - g);
      //analogWrite(LED1_BLUE, 255 - b);
      //digitalWrite(LED, msg[0] ? HIGH : LOW);
    }
    
    if (millis() - timer >= 1000) { // Send data every 1s
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
  /*
  Serial.println("loop");
  Serial.print(decodeState(motorL.getState()));
  Serial.print(" ");
  Serial.println(decodeState(motorL.getDirection()));
  // set direction to forward and release the brake in a single call
  motorL.run(FORWARD|RELEASE);
  motorR.run(FORWARD|RELEASE);
  Serial.print(decodeState(motorL.getState()));
  Serial.print(" ");
  Serial.println(decodeState(motorL.getDirection()));
  delay(1500);
  // switch directions and engage the brake
  motorL.run(BRAKE|BACKWARD);
  motorR.run(BRAKE|BACKWARD);
  Serial.print(decodeState(motor.getState()));
  Serial.print(" ");
  Serial.println(decodeState(motor.getDirection()));
  delay(1500);
  // release the brake now -- the motor should start running in the opposite direction
  motor.run(RELEASE);
  Serial.print(decodeState(motor.getState()));
  Serial.print(" ");
  Serial.println(decodeState(motor.getDirection()));
  delay(1500);
  motorL.setSpeed(0);
  motorR.setSpeed(0);
  delay(1500);
  // engage the brake again
  motor.run(BRAKE);
  motor.run(BRAKE);
  motorL.setSpeed(255);
  motorR.setSpeed(255);
  */
}

// helper function to print the motor's states in human-readable strings.
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

