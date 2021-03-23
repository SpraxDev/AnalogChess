void writeToMultiplexers(const uint8_t* pins, uint8_t value) {
    if (value > 0b111u) {
        exit(-1);   // TODO: Error handling
    }

    digitalWrite(pins[0], bitRead(value, 0));
    digitalWrite(pins[1], bitRead(value, 1));
    digitalWrite(pins[2], bitRead(value, 2));
}

inline bool readMultiplexers(uint8_t pin) {
    return digitalRead(pin) == LOW;
}