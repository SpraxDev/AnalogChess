#include <FastLED.h>

CRGB leds[64];

// {s0, s1, s2}
const uint8_t a[]{11, 10, 9}; // level. 120 mafia boss multiplexer
const uint8_t b[]{PIN7, PIN6, PIN5}; // lvl. 1 crook multiplexer
const uint8_t readPin = 12;
const uint8_t debugPin = PIN3;

void writeToMultiplexers(const uint8_t* pins, uint8_t value) {
    if (value > 0b111u) {
        Serial.println("ERR");
    }

    for (auto i = 0; i < 3; ++i) {
        digitalWrite(pins[i], bitRead(value, i));
    }
}

void readBoard(bool* result) {
    int ss = 0;
    for (auto x = 0; x < 8; ++x) {
        writeToMultiplexers(a, x);

        for (auto y = 0; y < 8; ++y) {
            writeToMultiplexers(b, y);

            result[ss] = digitalRead(readPin) == LOW;
            ++ss;
        }
    }
}

void onSYNC() {
    bool board[64];
    readBoard(board);

//    for (int i = 0; i < 8; ++i) {
//        uint8_t data = 0;
//
//        for (int j = 0; j < 8; ++j) {
//            bitWrite(data, j, board[i * 8 + j]);
//        }
//        // TODO: Send byte as bitmask
//    }

    // Print bitmask as text
    Serial.print("State: ");
    for (auto i = 0; i < sizeof(board); ++i) {
        Serial.print(board[i] ? 1 : 0);

        if (i % 8 == 0) {
            Serial.print(" ");
        }
    }
    Serial.println();
}

void setup() {
    for (int i = 0; i < 3; ++i) {
        pinMode(a[i], OUTPUT);
        pinMode(b[i], OUTPUT);
    }

    pinMode(readPin, INPUT_PULLUP);
    pinMode(debugPin, INPUT_PULLUP);

    CFastLED::addLeds<NEOPIXEL, PIN2>(leds, sizeof(leds));

    Serial.begin(9600);
}

void loop() {
    if (digitalRead(debugPin) == LOW) {
        Serial.println("LED...");
        for (auto i = 0; i < 64; ++i) {
            leds[i] = CRGB::Orange;
            FastLED.show();
            delay(80);

            leds[i] = CRGB::Black;
        }
    }

    onSYNC();
    delay(1000);
}

void serialEvent() {
    /*
      int i = 0;

      while (Serial.available() > 0) {
      Serial.read();
      i++;
      }

      if (i > 0) {
      Serial.print("Received ");
      Serial.print(i);
      Serial.println(" byte");
      }
    */
}