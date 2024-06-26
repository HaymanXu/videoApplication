# Basic function with minimal dependencies (Java)

![Architecture](/sample-apps/java-basic/images/sample-java-basic.png)

The project source includes function code and supporting resources:
- `src/main` - A Java function.
- `src/test` - A unit test and helper classes.
- `template.yml` - An AWS CloudFormation template that creates an application.
- `pom.xml` - A Maven build file.
- `1-create-bucket.sh`, `2-deploy.sh`, etc. - Shell scripts that use the AWS CLI to deploy and manage the application.

Use the following instructions to deploy the sample application.

# Requirements
- [Java 17 runtime environment (SE JRE)](https://www.oracle.com/java/technologies/javase-downloads.html)
- [Maven 3](https://maven.apache.org/docs/history.html)
- The Bash shell. For Linux and macOS, this is included by default. In Windows 10, you can install the [Windows Subsystem for Linux](https://docs.microsoft.com/en-us/windows/wsl/install-win10) to get a Windows-integrated version of Ubuntu and Bash.
- [The AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) v1.17 or newer.

# Setup
Download or clone this repository.

    $ git clone https://github.com/awsdocs/aws-lambda-developer-guide.git
    $ cd aws-lambda-developer-guide/sample-apps/videoApplication

To create a new bucket for deployment artifacts, run `./1-create-bucket.sh`.

    videoApplication$ ./1-create-bucket.sh
    make_bucket: lambda-artifacts-code-674aaaa08a5ddc85
    make_bucket: lambda-artifacts-videos-8e3120faaeeb2e31
    (If encounter permission denied, please run `chmod +x ./1-create-bucket.sh` first)

# Deploy
To deploy the application, run `2-deploy.sh`.

    java17-examples$ ./2-deploy.sh
    BUILD SUCCESSFUL in 1s
    Successfully packaged artifacts and wrote output template to file out.yml.
    Waiting for changeset to be created..
    Successfully created/updated stack - java17-examples

This script uses AWS CloudFormation to deploy the Lambda functions and an IAM role. If the AWS CloudFormation stack that contains the resources already exists, the script updates it with any changes to the template or function code.

You can also build the application with Maven. To use maven, add `mvn` to the command.

    java17-examples$ ./2-deploy.sh mvn
    [INFO] Scanning for projects...
    [INFO] -----------------------< com.example:java17-examples >-----------------------
    [INFO] Building java17-examples-function 1.0-SNAPSHOT
    [INFO] --------------------------------[ jar ]---------------------------------
    ...

(If encounter permission denied, please run `chmod +x ./2-deploy.sh mvn` first)
When make a POST api call, need to set Content-Type in request Headers in postman (eg: Accept : image/png).
When make a Get api call, need to set Accept in request Headers in postman (eg: Accept : image/png).

Redeploy
1. run `mvn clean package`.
2. run `2-deploy.sh`.

# Cleanup
To delete the application, run `./4-cleanup.sh`.

    videoApplication$ ./4-cleanup.sh