# Installation of Eclipse Kura™ on systems with snap package support

This is a guide for installing Kura on snappy systems, e.g. Ubuntu Core

Download Ubuntu for board of your choice. Ubuntu core is preferred system for network version of Kura™.
Snap packaging is also supported in classic Ubuntu systems.
```
https://www.ubuntu.com/download/iot
```

For other linux distributions follow see https://docs.snapcraft.io/installing-snapd/6735

## Install Kura™ / Kura™-nn

There are 4 risk channels to choose from (stable, candidate, beta, edge),
stable being default if no channel is specified at install time.
Available versions in channels can be checked with:
```
snap info kura
snap info kura-nn
```

Install Kura:
```
sudo snap install kura [ --<channel> , e.g. --candidate]
```
Install Kura-nn:
```
sudo snap install kura-nn [ --<channel>, e.g. --candidate]
```

After the installation is complete, connect all interfaces by executing:
```
sudo /snap/kura/current/connect-interfaces
sudo snap restart kura
```
or for kura-nn
```
sudo /snap/kura-nn/current/connect-interfaces
sudo snap restart kura-nn
```
