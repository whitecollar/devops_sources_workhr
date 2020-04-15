# postinstall.sh created from Mitchell's official lucid32/64 baseboxes

date > /etc/packer_build_time


# Apt-install various things necessary for Ruby, guest additions,
# etc., and remove optional things to trim down the machine.
echo 'deb http://www.rabbitmq.com/debian/ testing main' |
     sudo tee /etc/apt/sources.list.d/rabbitmq.list

wget -O- https://www.rabbitmq.com/rabbitmq-release-signing-key.asc |
     sudo apt-key add -


apt-get -y update
apt-get -y upgrade
apt-get -y install vim curl mc rabbitmq-server
apt-get clean
apt-get clean



# Installing the virtualbox guest additions
#apt-get -y install dkms
#VBOX_VERSION=$(cat /home/lex/.vbox_version)
#cd /tmp
#wget http://download.virtualbox.org/virtualbox/$VBOX_VERSION/VBoxGuestAdditions_$VBOX_VERSION.iso
#mount -o loop VBoxGuestAdditions_$VBOX_VERSION.iso /mnt
#sh /mnt/VBoxLinuxAdditions.run
#umount /mnt

#rm VBoxGuestAdditions_$VBOX_VERSION.iso

rabbitmq-plugins enable rabbitmq_management


# Zero out the free space to save space in the final image:
#dd if=/dev/zero of=/EMPTY bs=1M
#rm -f /EMPTYBB

# Removing leftover leases and persistent rules
echo "cleaning up dhcp leases"
#rm /var/lib/dhcp3/*

# Make sure Udev doesn't block our network
# http://6.ptmc.org/?p=164
echo "cleaning up udev rules"
#rm /etc/udev/rules.d/70-persistent-net.rules
#mkdir /etc/udev/rules.d/70-persistent-net.rules
#rm -rf /dev/.udev/
#rm /lib/udev/rules.d/75-persistent-net-generator.rules

echo "VMTOOLS"
cd /root/
wget http://185.96.87.20/up/VMwareTools-10.0.9-3917699.tar
tar -xvf /root/VMwareTools-10.0.9-3917699.tar
cd /root/vmware-tools-distrib
/root/vmware-tools-distrib/vmware-install.pl -f -d

exit