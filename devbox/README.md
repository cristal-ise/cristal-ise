## Configure cristal-ise vagrant devbox

- default user: vagrant/vagrant
- `sudo passwd ubuntu` - Change password of root user
- Add umake bin to PATH in .profile if not there already

```shell
if [ -d "$HOME/.local/share/umake/bin" ] ; then
    PATH=/home/vagrant/.local/share/umake/bin:$PATH
fi
```

- jdk 11 does not have the ejb/corba libraries so configure linux to use java 8

```shell
  sudo update-alternatives --config java
  sudo update-alternatives --config javac
```

- `mkdir workspace; cd workspace`
- `git clone https://username@github.com/cristal-ise/cristal-ise.git`
- `cd cristal-ise; mvn install` - Do this step before any build in eclipse to download all maven dependencies

### Configure Eclipse

1. Install eclipse `umake ide exlipse` or download eclipse-inst
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
- `docker run -p 3306:3306 --name mysql5.6 -e MYSQL_ROOT_PASSWORD=cristal -e MYSQL_ROOT_HOST=% -d mysql/mysql-server:5.6 --innodb-large-prefix=ON --innodb-file-format=Barracuda --innodb-file-format-max=Barracuda` - Create and run the docker container (shall be done only once)
- `docker container ls -a` - Check the status of the container
- `docker stop mysql5.6` - Stop  mysql5.6 server
- `docker start mysql5.6`- Start mysql5.6 server
- Mysql Workbench is already installed

### INSTALL postgres 9.6 using docker:
- `docker pull postgres:9.6`
- `docker run -p 5432:5432 --name psql9.6 -e POSTGRES_PASSWORD=cristal -d postgres:9.6`
- `docker stop psql9.6`
- `docker start psql9.6`
- pgadmin 4 is already installed

