# Telecomenzi IR – aplicație Android

Aplicație gratuită (nu e publicată în magazin) pentru **clonarea telecomenzilor în infraroșu**
și o **bibliotecă** de telecomenzi. Vine cu telecomanda pentru **plafoniera Novecento** deja creată.

## Instalare
1. Copiază `TelecomenziIR.apk` pe telefon (sau descarcă-l din GitHub).
2. Deschide-l și permite „Instalare din surse necunoscute”.
3. Ai nevoie de un telefon **cu emițător IR** (ex. Xiaomi / Redmi / POCO, unele Huawei/Honor).
   Fără el, aplicația funcționează ca bibliotecă, dar nu poate trimite comenzi.

## Ce face
- **Bibliotecă**: telecomenzi pe categorii (Iluminat, TV, AC…), căutare, backup/restore `.json`.
- **Șabloane**: Plafonieră Novecento (Pornit/Oprit, luminozitate ±, alb cald/rece/neutru, noapte,
  temporizator, memorie), lustră generică, TV, aer condiționat, ventilator. Poți adăuga și edita butoane.
- **Clonare** – 3 metode:
  1. **Receptor USB**: Arduino Nano/ESP32 + senzor TSOP38238 conectat prin OTG
     (firmware în `hardware/ir_learner/ir_learner.ino`). Apeși butonul pe telecomanda originală și codul se copiază.
  2. **Căutător de coduri**: trimite pe rând comenzile NEC (cel mai folosit protocol la lămpi LED)
     până reacționează lampa – fără niciun hardware suplimentar.
  3. **Cod manual**: NEC, NECext, Samsung32, RC5/RC5X, RC6, SIRC, Pronto HEX sau durate brute.
- **Import/Export** în formatul Flipper Zero `.ir` (există colecții publice mari cu astfel de fișiere).

## Important pentru plafoniera Novecento
Mai întâi verifică dacă telecomanda e **IR**: îndreapt-o spre camera telefonului și apasă un buton.
Dacă vezi o lumină mov care clipește, e IR și poate fi clonată. Dacă nu vezi nimic, e o telecomandă
**radio (2.4 GHz / 433 MHz)**, pe care niciun telefon nu o poate imita fără un modul radio extern.

## Compilare din sursă
```
./gradlew assembleRelease   # necesită Android SDK 35, JDK 17+
```
