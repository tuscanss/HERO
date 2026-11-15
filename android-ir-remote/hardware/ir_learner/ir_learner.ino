/*
 * Receptor IR pentru clonare – aplicația „Telecomenzi IR” (Android)
 *
 * Hardware (~25 lei):
 *   - Arduino Nano / Uno / Pro Micro  SAU  ESP32 / ESP8266
 *   - Senzor IR TSOP38238 / VS1838B (38 kHz)
 *       OUT -> pin IR_PIN, VCC -> 5V (sau 3.3V la ESP32), GND -> GND
 *   - Cablu USB + adaptor OTG pentru telefon
 *
 * Funcționare: măsoară duratele impulsurilor primite de la telecomandă și le
 * trimite pe portul serial (115200 baud), o linie per apăsare:
 *     IR 38000 9012 4480 580 540 580 1660 ...
 * (frecvența purtătoarei nu poate fi măsurată cu TSOP – se presupune 38 kHz,
 *  valoarea folosită de aproape toate telecomenzile de lămpi.)
 *
 * Nu necesită biblioteci externe.
 */

#if defined(ESP32) || defined(ESP8266)
  #define IR_PIN 14
  #define ISR_ATTR IRAM_ATTR
#else
  #define IR_PIN 2          // pin cu întrerupere pe Nano/Uno
  #define ISR_ATTR
#endif

#define MAX_EDGES 400
#define FRAME_GAP_US 30000UL   // pauza care încheie o captură

volatile uint16_t durations[MAX_EDGES];
volatile uint16_t count = 0;
volatile unsigned long lastEdge = 0;
volatile bool capturing = false;

void ISR_ATTR onEdge() {
  unsigned long now = micros();
  if (!capturing) {
    // Primul front: TSOP trage linia la LOW când primește purtătoare (început de puls)
    if (digitalRead(IR_PIN) == LOW) {
      capturing = true;
      count = 0;
      lastEdge = now;
    }
    return;
  }
  unsigned long d = now - lastEdge;
  lastEdge = now;
  if (count < MAX_EDGES) durations[count++] = d > 65535UL ? 65535 : (uint16_t)d;
}

void setup() {
  Serial.begin(115200);
  pinMode(IR_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(IR_PIN), onEdge, CHANGE);
  Serial.println("READY ir_learner v1");
}

void loop() {
  if (capturing && (micros() - lastEdge) > FRAME_GAP_US) {
    noInterrupts();
    uint16_t n = count;
    static uint16_t copy[MAX_EDGES];
    for (uint16_t i = 0; i < n; i++) copy[i] = durations[i];
    capturing = false;
    count = 0;
    interrupts();

    if (n >= 8) {           // ignorăm zgomotul
      Serial.print("IR 38000");
      for (uint16_t i = 0; i < n; i++) {
        Serial.print(' ');
        Serial.print(copy[i]);
      }
      Serial.println();
    }
  }
}
