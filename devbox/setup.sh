#!/bin/sh

# update the system
export DEBIAN_FRONTEND=noninteractive
apt-mark hold keyboard-configuration
apt-get update --fix-missing
apt-get -y upgrade
apt-mark unhold keyboard-configuration

################################################################################
# Install the mandatory tools
################################################################################

# install utilities
apt-get -y install vim git zip bzip2 fontconfig curl ca-certificates language-pack-en

# install Openjdk 8 max 10 (java 11 does not contain ejb/corba packages anymore)
apt-get install openjdk-8-jdk

# install Maven
apt-get install -y maven

# install Node.js
curl -sL https://deb.nodesource.com/setup_10.x | bash
apt-get install -y nodejs

# update NPM
npm install -g npm

# install NPM packagaes
#npm install -g @angular/cli

################################################################################
# Install the graphical environment
################################################################################

# force encoding
echo 'LANG=en_US.UTF-8' >> /etc/environment
echo 'LANGUAGE=en_US.UTF-8' >> /etc/environment
echo 'LC_ALL=en_US.UTF-8' >> /etc/environment
echo 'LC_CTYPE=en_US.UTF-8' >> /etc/environment

# run GUI as non-privileged user
echo 'allowed_users=anybody' > /etc/X11/Xwrapper.config

# install xubuntu desktop and VirtualBox guest tools
apt-get install -y xubuntu-desktop virtualbox-guest-dkms virtualbox-guest-utils virtualbox-guest-x11

# remove light-locker
apt-get remove -y light-locker --purge

################################################################################
# Install the development tools
################################################################################

# install Ubuntu Make - see https://wiki.ubuntu.com/ubuntu-make
apt-get install -y ubuntu-make

# install Chromium Browser
apt-get install -y chromium-browser

# install MySQL Workbench
apt-get install -y mysql-workbench

# install postgres packages for PgAdmin 4
curl https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -
sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
apt-get update
apt-get install -y pgadmin4

# install Guake
apt-get install -y guake
cp /usr/share/applications/guake.desktop /etc/xdg/autostart/

# install zsh
apt-get install -y zsh

# install oh-my-zsh
git clone git://github.com/robbyrussell/oh-my-zsh.git /home/vagrant/.oh-my-zsh
cp /home/vagrant/.oh-my-zsh/templates/zshrc.zsh-template /home/vagrant/.zshrc
chsh -s /bin/zsh vagrant
echo 'SHELL=/bin/zsh' >> /etc/environment

# change user to vagrant
chown -R vagrant:vagrant /home/vagrant/.zshrc /home/vagrant/.oh-my-zsh

################################################################################
# Install Docker together with cistal-ise and database images
################################################################################

# install latest Docker
curl -fsSL https://get.docker.io/ | sh

# install latest docker-compose
curl -L "$(curl -s https://api.github.com/repos/docker/compose/releases | grep browser_download_url | head -n 4 | grep Linux | grep -v sha256 | cut -d '"' -f 4)" > /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# configure docker group (docker commands can be launched without sudo)
usermod -aG docker vagrant

# fix ownership of home
chown -R vagrant:vagrant /home/vagrant/

################################################################################
# Install IDEs
################################################################################

# install the latest umake
add-apt-repository ppa:lyzardking/ubuntu-make
apt-get update
apt-get install ubuntu-make

# install Eclipse

# install Intellij

# install VSCode
wget -q https://packages.microsoft.com/keys/microsoft.asc -O- | apt-key add -
add-apt-repository "deb [arch=amd64] https://packages.microsoft.com/repos/vscode stable main"
apt-get update
apt-get install -y code

# install VSCode extensions
code --install-extension johnpapa.Angular2
code --install-extension msjsdiag.debugger-for-chrome
code --install-extension dbaeumer.vscode-eslint
code --install-extension EditorConfig.EditorConfig
code --install-extension PeterJausovec.vscode-docker
code --install-extension abusaidm.html-snippets
code --install-extension xabikos.JavaScriptSnippets
code --install-extension christian-kohler.npm-intellisense #intelligent js imports
code --install-extension alefragnani.project-manager #manage several vscode windows

################################################################################
# Clean the box
################################################################################

apt-get -y autoclean
apt-get -y clean
apt-get -y autoremove
dd if=/dev/zero of=/EMPTY bs=1M > /dev/null 2>&1
rm -f /EMPTY
