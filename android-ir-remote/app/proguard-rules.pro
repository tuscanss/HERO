# Modelele serializate cu kotlinx.serialization
-keep,includedescriptorclasses class ro.hero.irremote.**$$serializer { *; }
-keepclassmembers class ro.hero.irremote.** { *** Companion; kotlinx.serialization.KSerializer serializer(...); }
# Driverele USB-serial sunt găsite prin reflecție
-keep class com.hoho.android.usbserial.driver.** { *; }
