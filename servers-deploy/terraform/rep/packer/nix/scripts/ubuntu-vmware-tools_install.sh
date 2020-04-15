#!/bin/bash -eux

    echo "Installing VMware Tools"

    mkdir -p /mnt/hgfs
    apt-get update -y
    apt-get install -y open-vm-tools module-assistant linux-headers-$(uname -r) linux-image-linux-headers-$(uname -r) open-vm-tools-dkms
    module-assistant prepare
    module-assistant --text-mode -f get open-vm-tools-dkms

    apt-get -y remove linux-headers-$(uname -r) build-essential perl
    apt-get -y autoremove