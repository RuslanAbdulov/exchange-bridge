#! /bin/bash
sudo apt update
sudo apt install openjdk-21-jdk
git clone https://github.com/RuslanAbdulov/exchange-bridge.git
cd exchange-bridge/
#./gradlew tasks
#./gradlew clean build --no-daemon

#ssh root@165.227.48.202
mkdir ~/app
scp build/libs/exchange-bridge-0.0.1-SNAPSHOT.jar root@165.227.48.202:/root/app/
java -jar ~/app/exchange-bridge-0.0.1-SNAPSHOT.jar --spring.application.json=
