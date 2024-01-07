# Manual setup for development on ubuntu - executed on ubuntu mate 22.04.3 LTS

## if ping/wget does not work, try to disable ipv6 - https://itsfoss.com/disable-ipv6-ubuntu-linux
1. add these lines to /etc/sysctl.conf
```
net.ipv6.conf.all.disable_ipv6=1
net.ipv6.conf.default.disable_ipv6=1
net.ipv6.conf.lo.disable_ipv6=1
```
1. execute `sudo sysctl -p`

## Java, Maven install
1. `sudo apt install openjdk-8-jdk` - required for cristalise 5.x
1. `sudo apt install openjdk-17-jdk` - required for cristalise 6.x
1. `sudo apt install -y maven`
1. `wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -P /tmp`
1. `sudo tar xf /tmp/apache-maven-3.9.6-bin.tar.gz -C /opt`
1. `sudo update-alternatives --install /usr/bin/mvn mvn /opt/apache-maven-3.9.6/bin/mvn 396`

## Git, git-flow install and config, clone cristal-ise repo
1. `sudo apt install git git-flow`
1. `git config --global credential.helper cache`
1. `git config --global pull.rebase true`
1. `git config --global user.name 'Jean Doe'`
1. `git config --global user.email jean.doe@email.com`
1. `ssh-keygen -t ed25519 -C "zs.myth@gmail.com"`
1. add the content of ~/.ssh/id_ed25519.pub to github profile
1. `git clone git@github.com:cristal-ise/cristal-ise.git`

## Sublimetext - https://www.sublimetext.com/docs/linux_repositories.html
1. `wget -qO - https://download.sublimetext.com/sublimehq-pub.gpg | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/sublimehq-archive.gpg > /dev/null`
1. `echo "deb https://download.sublimetext.com/ apt/stable/" | sudo tee /etc/apt/sources.list.d/sublime-text.list`
1. `sudo apt-get update`
1. `sudo apt-get install sublime-text`

#
# Node Version Manager (nvm) - node, npm, angular
#
1. `curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash`
1. 'nvm install 10' - required for cristalise 5.x
1. 'nvm install 16' - required for cristalise 6.x


