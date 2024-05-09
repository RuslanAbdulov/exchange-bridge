#! /bin/bash
sudo apt update
sudo apt install openjdk-21-jdk
git clone https://github.com/RuslanAbdulov/exchange-bridge.git
cd exchange-bridge/
#./gradlew tasks
#./gradlew clean build --no-daemon

#ssh root@152.42.128.231
iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 9000

mkdir ~/app
scp build/libs/exchange-bridge-0.0.1-SNAPSHOT.jar root@152.42.128.231:/root/app/exchange-bridge.jar
cd app

