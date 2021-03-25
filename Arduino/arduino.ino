#include <Arduino.h>
//#include <FastLED.h>

#include "Utils.cpp"

//CRGB leds[64];

// {s0, s1, s2}
const uint8_t a[]{PIN_A1, PIN_A2, PIN_A3}; // level. 120 mafia boss multiplexer
const uint8_t b[]{PIN_A4, PIN_A5, PIN_A6}; // lvl. 1 crook multiplexer
const uint8_t readPin = PIN_A0;

char cmdBuffer[32]{};
const uint8_t cmdBufferSize = sizeof(cmdBuffer);
uint8_t cmdBufferIndex = 0;

void onLED(const char* args) {
    uint8_t ledId = args[0];
    uint32_t color = (*(uint32_t*) (args + 1)) & 0x00FFFFFF;

//    leds[ledId] = color;
}

void onLEDSHOW(const char* args) {
//    FastLED.show();
}

void onSYNC(const char* args) {
    bool board[64];
    readBoard(board);

    for (int i = 0; i < 8; ++i) {
        uint8_t data = 0;

        for (int j = 0; j < 8; ++j) {
            bitWrite(data, j, board[i * 8 + j]);
        }

        Serial.write(data);
    }

    for (bool i : board) {
        Serial.write(i ? 1 : 0);
    }
}

void onRESET(const char*) {
    // reset buffer
    cmdBufferIndex = 0;
    memset(cmdBuffer, 0, sizeof(cmdBuffer));
}

void readBoard(bool* result) {
    for (uint8_t x = 0; x < 8; ++x) {
        writeToMultiplexers(b, x);

        for (uint8_t y = 0; y < 8; ++y) {
            writeToMultiplexers(a, y);

            result[x + (y * 8)] = readMultiplexers(readPin);
        }
    }
}

struct Command {
    const char* name;

    void (* callback)(const char*);
};

const Command commands[]{
        {"RST",      &onRESET},
        {"SYNC",     &onSYNC},
        {"LED SHOW", &onLEDSHOW},
        {"LED",      &onLED}
};

void onCmd() {
    for (const auto& cmd : commands) {
        size_t len = strlen(cmd.name);
        size_t i = 0;

        for (; i < len; ++i) {
            if (cmd.name[i] != cmdBuffer[i]) {
                break;
            }
        }

        if (i == (len - 1)) {
            Serial.write(6);
            Serial.write(66);
            cmd.callback(cmdBuffer + len);

            return;
        }
    }

    Serial.write(21); // no matching cmd found
}

void setup() {
    // Multiplexer
    for (int i = 0; i < 3; ++i) {
        pinMode(a[i], OUTPUT);
        pinMode(b[i], OUTPUT);
    }

    pinMode(readPin, INPUT_PULLUP);

    // LEDs
//    CFastLED::addLeds<NEOPIXEL, PIN6>(leds, sizeof(leds));

    // Serial
    Serial.begin(14400);
}

void serialEvent() {
    while (Serial.available() > 0) {
        int inChar = Serial.read();

        if (cmdBufferIndex > cmdBufferSize) [[unlikely]] {  // Buffer overflow
            if (inChar == 4) {
                Serial.write(24);   // CAN (Cancel)
                Serial.write(4);    // EOT
                Serial.flush();

                // reset buffer
                cmdBufferIndex = 0;
                memset(cmdBuffer, 0, sizeof(cmdBuffer));
            }
        } else {
            if (inChar != 4) {  // EOT (End Of Transmission)
                cmdBuffer[cmdBufferIndex++] = (char) inChar;
            } else {
                onCmd();
                Serial.write(4);
                Serial.flush();

                // reset buffer
                cmdBufferIndex = 0;
                memset(cmdBuffer, 0, sizeof(cmdBuffer));
            }
        }

        Serial.flush();
    }
}

void loop() {
}