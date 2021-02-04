#include <Arduino.h>

const uint8_t PIN_MUX_X_Z = 2;
const uint8_t PIN_MUX_X_S0 = 3;
const uint8_t PIN_MUX_X_S1 = 4;
const uint8_t PIN_MUX_X_S2 = 5;

void setup() {
    pinMode(PIN_MUX_X_Z, INPUT_PULLUP);

    pinMode(PIN_MUX_X_S0, OUTPUT);
    pinMode(PIN_MUX_X_S1, OUTPUT);
    pinMode(PIN_MUX_X_S2, OUTPUT);

//    pinMode(PIN_MUX_Y_S1, OUTPUT);
//    pinMode(PIN_MUX_Y_S2, OUTPUT);
//    pinMode(PIN_MUX_Y_S3, OUTPUT);

    Serial.begin(14400);

    Serial.println("Prüfe 0 (0) - Cycle mit '.'");
}

bool isOccupied(uint8_t x, uint8_t y) {
    if (x > 0b111u || y > 0b111u) {
        exit(-1);  // TODO: Error logging
    }

    digitalWrite(PIN_MUX_X_S0, bitRead(x, 0));
    digitalWrite(PIN_MUX_X_S1, bitRead(x, 1));
    digitalWrite(PIN_MUX_X_S2, bitRead(x, 2));

//    digitalWrite(PIN_MUX_Y_S0, bitRead(y, 0));
//    digitalWrite(PIN_MUX_Y_S1, bitRead(y, 0));
//    digitalWrite(PIN_MUX_Y_S2, bitRead(y, 0));

    return digitalRead(PIN_MUX_X_Z) == LOW;
}

uint8_t mPin = 0;

void loop() {
    if (Serial.read() == '.') {
        ++mPin;

        if (mPin > 0b111u) {
            mPin = 0;
        }

        Serial.print("Prüfe ");
        Serial.print(mPin);
        Serial.print(" (");
        Serial.print(mPin, BIN);
        Serial.println(")");
    }

    digitalWrite(LED_BUILTIN, isOccupied(mPin, 0) ? HIGH : LOW);
}