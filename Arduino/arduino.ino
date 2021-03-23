#include <Arduino.h>
#include <FastLED.h>

#include "Utils.cpp"

CRGB leds[64];

// {s0, s1, s2}
const uint8_t a[]{3, 4, 5}; // level. 120 mafia boss multiplexer
const uint8_t b[]{6, 7, 8}; // lvl. 1 crook multiplexer
const uint8_t readPin = 9;

char cmdBuffer[32]{};
const uint8_t cmdBufferSize = sizeof(cmdBuffer);
uint8_t cmdBufferIndex = 0;

void onLED(const char* args) {
    uint8_t ledId = args[0];
    uint32_t color = (*(uint32_t*) (args + 1)) & 0x00FFFFFF;

    leds[ledId] = color;

    Serial.write(6);
}

void onLEDSHOW(const char* args) {
    FastLED.show();

    Serial.write(6);
}

void onSYNC(const char* args) {
    Serial.write(6);

    bool board[64];
    readBoard(board);

    for (int i = 0; i < 8; ++i) {
        uint8_t data = 0;

        for (int j = 0; j < 8; ++j) {
            bitWrite(data, j, board[i * 8 + j]);
        }

        Serial.write(data);
    }
}

// TODO: maybe remove idk
void onSTATUS(const char*) {
    Serial.write(6);
    Serial.println("Am fine, thanks bro.");
}

void readBoard(bool* result) {
    for (uint8_t y = 0; y < 8; ++y) {
        writeToMultiplexers(a, y);

        for (uint8_t x = 0; x < 8; ++x) {
            writeToMultiplexers(b, x);

            result[x + (y * 8)] = readMultiplexers(readPin);
        }
    }
}

void setup() {
    // Multiplexer
    for (int i = 0; i < 3; ++i) {
        pinMode(a[i], OUTPUT);
        pinMode(b[i], OUTPUT);
    }

    pinMode(readPin, INPUT_PULLUP);

    // LEDs
    CFastLED::addLeds<NEOPIXEL, 6>(leds, 64);

    // Serial
    Serial.begin(14400);
}

void loop() {
}

void serialEvent() {
    while (Serial.available()) {
        int inChar = Serial.read();

        if (cmdBufferIndex > cmdBufferSize) [[unlikely]] {  // Buffer overflow
            if (inChar == 3) {
                Serial.write(24);   // CAN (Cancel)
                Serial.write(4);    // EOT

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

                // reset buffer
                cmdBufferIndex = 0;
                memset(cmdBuffer, 0, sizeof(cmdBuffer));
            }
        }
    }
}

struct Command {
    const char* name;

    void (* callback)(const char*);
};

const Command commands[]{
        {"LED SHOW", &onLEDSHOW},
        {"LED",      &onLED},
        {"SYNC",     &onSYNC},
        {"STATUS",   &onSTATUS} // TODO
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

        if (i == len) {
            cmd.callback(cmdBuffer + len);
            return;
        }
    }

    Serial.write(21); // no matching cmd found
}