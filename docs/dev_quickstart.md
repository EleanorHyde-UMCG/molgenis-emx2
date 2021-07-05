# Quickstart / Pull request review

Below the steps to checkout the code, optionally a specific branch, build, and then look at the current functionality.

## Clone the code

Clone latest version of sourcecode from github using [git](https://git-scm.com/downloads)

```
git clone git@github.com:molgenis/molgenis-emx2.git
```

Optionally, checkout the branch you like to review

```
cd molgenis-emx
git checkout <branch name here>
```

Then you can either build + run the whole molgenis.jar, or only one app, described below. Or you can run inside
IntelliJ.

## Build whole system

Requires [Postgresql 13](https://www.postgresql.org/download/) and java (we use
adopt [OpenJDK 16](https://adoptopenjdk.net/)):

* Optionally, drop a previous version of molgenis database (caution: destroys previous data!)
  ```console
  sudo -u postgres psql
  postgres=# drop database molgenis;
  ```
* If not already done, create postgresql database with name 'molgenis' and with superadmin user/pass 'molgenis'. On
  Linux/Mac commandline:
  ```console
  sudo -u postgres psql
  postgres=# create database molgenis;
  postgres=# create user molgenis with superuser encrypted password 'molgenis';
  postgres=# grant all privileges on database molgenis to molgenis;
  ```
* change into molgenis-emx2 directory and then compile and run via command
   ```
   cd molgenis-emx2
   ./gradlew run
   ```
* View the result on http://localhost:8080

Alternatively you can run inside [IntelliJ IDEA](https://www.jetbrains.com/idea/). Then instead of last ./gradlew step:

* Open IntelliJ and open molgenis-emx2 directory
* IntelliJ will recognize this is a gradle project and will build
* navigate to `backend/molgenis-emx2-run/src/main/java/org/molgenis/emx2'
* Right click on `RunMolgenisEmx2Full` and select 'run'

## Build one 'app'

Requires only [docker compose](https://docs.docker.com/compose/) and [yarn 1.x](https://yarnpkg.com/)

* Start molgenis using docker-compose
  ```console
  cd molgenis-emx2
  docker-compose up
  ```
  You can verify that it runs by looking at http://localhost:8080
* Build the app workspace as a whole
  ```console
  cd apps
  yarn install
  ```
* Serve only the app you want to look at
  ```console
  cd <yourapp>
  yarn serve
  ```
  Typically the app is then served at http://localhost:9090 (look at the console to see actual port number)

## Tips

last updated 15 nov 2020

### IntelliJ plugins

* We use IntelliJ 2020.2 with
    * vue plugin
    * google-java-format plugin
    * prettier plugin, set run for files to include '.vue' and 'on save'
    * auto save and auto format using 'save actions' plugin

### Pre-commit hook

We use pre-commit build hook in .git/hooks/pre-push to ensure we don't push stuff that breaks the build.

```
./gradlew test --info
RESULTS=$?
if[$RESULTS -ne 0]; then
    exit 1
fi
exit 0
```

### Reset gradle cache/deamon

Sometimes it help to reset gradle cache and stop the gradle daemon

```
./gradlew --stop rm -rf $HOME/.gradle/
```

### Delete all schemas tool

If you want to delete all schemas in database run
```molgenis-emx2-sql/test/java/.../AToolToCleanDatabase```




