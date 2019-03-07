# Installation of Eclipse Kura™ on Intel Up²

This is a guide for installing Kura on Ubuntu Core systems

## Install Kura

There are 4 risk channels to choose from (stable, candidate, beta, edge), stable being default
Available versions in channels can be checked with:
```
snap info kura-nn
```

Install Kura:
```
sudo snap install kura-nn [ --<channel>]
```
After the installation is complete, connect all interfaces by executing:
```
sudo /snap/kura-nn/current/connect-interfaces
sudo snap restart kura-nn
```
