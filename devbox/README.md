This work is based on [jhipster-devbox](https://github.com/jhipster/jhipster-devbox).

## Create cristal-ise vagrant devbox

1. Install vagrant on your pc
1. On the host machine clone this repository: `git clone https://john.doe@github.com/cristal-ise/cristal-ise.git`
1. `vagrant up`
- Alternatively use eclipe vagrant tools

## Configure cristal-ise vagrant devbox

1. default user: vagrant/vagrant
1. `sudo passwd ubuntu` - Change password of root user
1. Follow this guide to update VirtualBox Guest Additions
    - https://linuxize.com/post/how-to-install-virtualbox-guest-additions-in-ubuntu/
1. `sudo adduser $USER vboxsf` - Add permission to use shared folders with the host OS
1. Consider to use different oh-my-zshell theme like: powerlevel10k
   1. edit ~/.zshrc file: `ZSH_THEME="powerlevel10k/powerlevel10k"`
   1. use `p10k configure` if you want to tune the settings
1. `git config --global credential.helper cache`
1. `git config --global user.name 'John Doe'`
1. `git config --global user.email john.doe@email.com`
1. `git config --global pull.rebase true`
1. Add umake bin to PATH in .profile if not there already

    ```shell
    if [ -d "$HOME/.local/share/umake/bin" ] ; then
        PATH=/home/vagrant/.local/share/umake/bin:$PATH
    fi
    ```

1. jdk 11 does not have the ejb/corba libraries so configure linux to use java 8

    ```shell
      sudo update-alternatives --config java
      sudo update-alternatives --config javac
    ```

1. `mkdir workspace; cd workspace`
1. `git clone https://john.doe@github.com/cristal-ise/cristal-ise.git`
1. `cd cristal-ise;`
1. `git checkout -t origin/master`
1. `git flow init`
1. `mvn install` - Do this step before any build in eclipse to download all maven dependencies
1. Rebbot the VM

### Configure Eclipse

1. Install eclipse `umake ide eclipse` or download eclipse-inst
1. Add lombok to eclipse: https://projectlombok.org/download
1. Open Eclipse and use workspace: `~/workspace/cristal-ise`
1. Add groovy plugin to eclipse:  https://dist.springsource.org/release/GRECLIPSE/e4.14
1. Configure eclipse to use groovy 2.5
1. 'Import/Existing Maven Projects' - select directory `~/workspace/cristal-ise`
1. The flattener maven plugin has to be ignored for all imported projects in eclipse preferences

### INSTALL mysql 5.6 using docker:

- `docker pull mysql/mysql-server:5.6` - install mysql 5.6 image
-  `docker image ls` - Check install is correct
- Read this manual: https://dev.mysql.com/doc/mysql-installation-excerpt/5.5/en/docker-mysql-getting-started.html
- Create and run the docker container (shall be done only once)
  - `docker run -p 3306:3306 --name mysql5.6 -e MYSQL_ROOT_PASSWORD=cristal -e MYSQL_ROOT_HOST=% -d mysql/mysql-server:5.6 --innodb-large-prefix=ON --innodb-file-format=Barracuda --innodb-file-format-max=Barracuda`
- `docker container ls -a` - Check the status of the container
- `docker stop mysql5.6` - Stop  mysql5.6 server
- `docker start mysql5.6`- Start mysql5.6 server
- Mysql Workbench is already installed

### INSTALL postgres 9.6 using docker:
- `docker pull postgres:9.6`
- Create and run the docker container (shall be done only once)
  - `docker run -p 5432:5432 --name psql9.6 -e POSTGRES_PASSWORD=cristal -d postgres:9.6`
- `docker stop psql9.6`
- `docker start psql9.6`
- pgadmin 4 is already installed

### INSTALL older version of Google Chrome:
Development migth require to use a specific version of Google Chrome. Download the Ubuntu package from here:
https://www.slimjet.com/chrome/google-chrome-old-version.php
