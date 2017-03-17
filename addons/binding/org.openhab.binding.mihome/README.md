---
layout: documentation
---

{% include base.html %}

# Xiaomi Mi Smart Home Binding

The binding allows your openHAB to communicate with Xiaomi Smart Home Suite. 

In order to connect the Gateway, you need to install MiHome app 
from the [Google Play](https://play.google.com/store/apps/details?id=com.xiaomi.smarthome) or [AppStore](https://itunes.apple.com/app/mi-home-xiaomi-for-your-smarthome/id957323480).

## How to set up

1. Install the binding
2. Setup Gateway to be discoverable
   1. Add Gateway 2 to your WiFi Network
   2. Install Mi Home app from [Google Play](https://play.google.com/store/apps/details?id=com.xiaomi.smarthome) or [AppStore](https://itunes.apple.com/app/mi-home-xiaomi-for-your-smarthome/id957323480)
   3. Set your region to Mainland China under Settings -> Locale (seems to be required)
   4. Update gateway (maybe multiple times)
   5. Enable developer mode:<br />
      1. Select your Gateway in Mi Home
      2. Go to the "..." menu and click "About"
      3. Tap the version number "Version : 2.XX" at the bottom of the screen repeatedly until you enable developer mode
      4. you should now have 2 extra options listed in Chinese.
      5. Choose the first new option (fourth position in the menu or the longer text in Chinese)
      6. Tap the toggle switch to enable LAN functions. Note down the developer key (something like: 91bg8zfkf9vd6uw7).
      7. Make sure you hit the OK button (to the right of the cancel button) to save your changes.
4. In openHAB you should now be able to discover the Xiaomi Gateway
5. Enter the previously noted developer key in openHAB Paper UI -> Configuration -> Things -> Xiaomi Gateway -> Edit -> Developer Key. Save.
   (This is required if you want to be able to send controls to the devices like the light of the gateway.)
6. Your sensors should be getting discovered by openHAB as you add and use them.

## Connecting sub-devices (sensors) to the Gateway

There are two ways of connecting Xiaomi devices to the gateway:

1. Online - within the MiHome App
2. Offline - manual

    * Click 3 times on the Gateway's button
    * Gateway will flash in blue and you will hear female voice in Chinese
    * Place the needle into the sensor and hold it for at least 3 seconds
    * You'll hear confirmation message in Chinese 
    
## Important information

The binding requires port `9898` to not be used by any other service on the system.

## Full Example

xiaomi.items:
```
TODO
```

xiaomi.rules:
```
rule "Xiaomi Switch single click"
when
    Channel "mihome:sensor_switch:<id>:button" triggered CLICK
then
    if (Vacuum_Dock.state == ON) {
        sendCommand(Vacuum_Dock, OFF)
    } else {
        sendCommand(Vacuum_Dock, ON)
    }
end

rule "Xiaomi Switch double click"
when
    Channel "mihome:sensor_switch:<id>:button" triggered DOUBLE_CLICK
then
    if (Gateway_Light.state > 0) {
        sendCommand(Gateway_Light, OFF)
    } else {
        sendCommand(Gateway_Light, ON)
    }
end


rule "Bedroom light control with cube"
when
  Channel 'mihome:sensor_cube:<id>:action' triggered
then
  if (receivedEvent.event == "SHAKE_AIR") {
    if (LEDBulb_Brightness.state == 0) {
      sendCommand(LEDBulb_Brightness, ON)
      sendCommand(LEDBulb_ColorTemperature, 50)
    } else {
      sendCommand(LEDBulb_Brightness, OFF)
    }
  } else if (receivedEvent.event == "FLIP90") {
    sendCommand(LEDBulb_Brightness, ON)
    sendCommand(LEDBulb_ColorTemperature, 10)
  } else if (receivedEvent.event == "FLIP180") {
    sendCommand(LEDBulb_Brightness, ON)
    sendCommand(LEDBulb_Color, new HSBType(new DecimalType(90),new PercentType(100),new PercentType(100)))
  }
end


rule "Hallway night light"
when
  Item MotionSensor_MotionStatus changed from OFF to ON or
  Item MotionSensor_MotionStatus changed from ON to OFF
then
  if (MotionSensor_MotionStatus.state == ON) {
    sendCommand(Gateway_Brightness, new PercentType(2))
    Thread::sleep(100)
    sendCommand(Gateway_Color, HSBType.fromRGB(i255, 241, 224))
  } else {
    sendCommand(Gateway_Brightness, new PercentType(0))
  }
end
```