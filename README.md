# Simple Dashboard (v0.3)

An Android-based digital dashboard application designed for sim racing enthusiasts. It receives real-time telemetry via UDP broadcast and displays critical racing data on your mobile device, mimicking a professional rally/race car display.

## Features
* **Dynamic Shift Lights:** 16-LED RPM bar (Green -> Red -> Blinking Blue at shift point) built with absolute precision.
* **RPM & Speed Tracking:** Massive, easily readable speed indicator and precise RPM readings.
* **Gear Display:** Huge gear indicator (turns red when Reverse is engaged).
* **Race Timer:** High-precision lap/stage timer (`MM:SS:ms`).
* **Engine Monitoring:** Tracks coolant temperature (with automatic Kelvin-to-Celsius conversion under the hood).
* **Connection Watchdog:** Automatically switches to a "Waiting for data..." screen with an animated spinner if telemetry drops for more than 1 second.

## Tech Stack
* **Platform:** Android 8+
* **UI Framework:** Jetpack Compose (Modern, fully declarative UI with smooth `rememberInfiniteTransition` animations)
* **Asynchrony:** Kotlin Coroutines & Flows
* **Network:** UDP Socket Listener

## Supported Projects:
* Gran Turismo 7
* Assetto Corsa
* Richard Burns Rally
* SimHub

## How to
* The app runs a background loop listening for incoming UDP telemetry packets from the simulation game. It decodes the byte buffer, handles data validation, and updates the Compose state in real time. 
* **Gran Turismo 7** - ports 33739 (heartbeath) and 33740 (telemetry) should be opened
* **Assetto Corsa** - need your PC IP address for the app. port 9996 should be opened in the brandmauer
* **Richard Burns Rally** - port 6776 should be opened in the brandmauer
* **SimHub** - need your PC IP address and port (Web Server port TCP) for the app
