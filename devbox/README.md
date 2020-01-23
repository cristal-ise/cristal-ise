### INSTALL mysql 5.6 using docker:

- `docker pull mysql/mysql-server:5.6` - install mysql 5.6 image
-  `docker image ls` - Check install is correct
- Read this manual: https://dev.mysql.com/doc/mysql-installation-excerpt/5.5/en/docker-mysql-getting-started.html
- `docker run -p 3306:3306 --name mysql5.6 -e MYSQL_ROOT_PASSWORD=cristal -e MYSQL_ROOT_HOST=% -d mysql/mysql-server:5.6 --innodb-large-prefix=ON --innodb-file-format=Barracuda --innodb-file-format-max=Barracuda` - Create and run the docker container (shall be done only once)
- `docker container ls -a` - Check the status of the container
- `docker stop mysql5.6` - Stop  mysql5.6 server
- `docker start mysql5.6`- Start mysql5.6 server

### INSTALL postgres 9.6 using docker:
- `docker pull postgres:9.6`
- `docker run -p 5432:5432 --name psql9.6 -e POSTGRES_PASSWORD=cristal -d postgres:9.6`
- `docker stop psql9.6`
- `docker start psql9.6`

### Configure cristal-ise vagrant devbox

- default user: vagrant/vagrant
- Change password of root user
    `sudo passwd ubuntu`
- Add umake bin to PATH in .profile

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
- `cd cristal-ise; mvn install` - Before any build in eclipse download all maven dependencies
- install eclipse `umake ide exlipse` or download eclipse-inst
- add groovy plugin to eclipse:  https://dist.springsource.org/release/GRECLIPSE/e4.14
- configure eclipse to use groovy 2.5
- add lombok to eclipse
