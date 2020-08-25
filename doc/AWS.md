# Deployment of SimpleAnnotationServer (SAS) on Amazon Web Services (AWS)

The aim of this guide is to give information on how to host the SimpleAnnotationServer (SAS) in the Cloud using Amazon Web Services (AWS). The setup allows the management of SAS in GitHub and the automatic deployment to AWS when changes are made to the `master` branch. This is achieved through the use of Docker and hosting the Docker instance in Amazon's Elastic Container Service (ECS). The SAS Cloud service is made up of the following four components: 

**Deployment**
This does the following:
 * Monitor GitHub for changes to the master branch
 * Build a Docker Image
 * Store the image using the AWS Image Repository (part of ECS)

**Management**
This component is responsible for taking a Docker image and making it available as an ECS service. A ECS service can scale instances to cope with load and also monitors errors and replaces instances which are no longer healthy. In the diagram below this part is split into two:

 * Amazon ECS - Service
   * Responsible for managing Docker Images (ECS Repository)
   * Responsible for creating ECS Tasks that have running containers built from Docker Images
   * A Service that manages scaling and health monitoring of ECS Tasks
 * Amazon ECS - Cluster
   * A Cluster is responsible for running a service 
   * A cluster has assigned machines either a EC2 machine or Fargate.

**Storage**
It is possible to setup SAS with a number of backends that could be hosted using AWS but in this guide the storage will be an ElasticSearch instance as this is one of the available managed services through AWS. A managed service means Amazon will take care of backup and upgrades to the service. 

**Access**
The access component uses an Elastic Load Balancer to coordinate the incoming requests to an available ECS Service. The ECS service looks after the process of adding tasks to a Target Group. The Elastic Load Balancer manages the incoming requests and sends them to one of the ECS tasks that is in the Target group. 

A diagram of these components can be seen below:

![SAS in the Cloud](images/SAS_on_AWS.jpg)

## Rough Costs


## Setup

To ensure the components work together it is important to do the following steps in order. The AWS interfaces change regularly so rather than doing specific screen shots and specific instructions this guide gives information on the important fields and what they are used for. The setup detailed here is the cheapest setup that will work but notes will be given where it is possible to scale up the infrastructure if this is required. 

### Storage - Elastic Search Service

Before creating a Elastic Search domain we must create a user. To do this go to the Identity and Access Management (IAM) part of the AWS console. Add a User with **Programmatic access* and attach the `AmazonESReadOnlyAccess` policy. The Policy contains the following JSON:

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": [
                "es:Describe*",
                "es:List*",
                "es:Get*"
            ],
            "Effect": "Allow",
            "Resource": "*"
        }
    ]
}
```

When creating the user make note of the **Access Key** and **Secret Access Key** as this will be required when configuring the SAS container. Also make note of the user's ARN which can be copied from the User Summary screen after the user has been created. 

Next we need to create the ElasticSearch service by going to the AWS `Elasticsearch Service` in the list of AWS services. To use SAS we need to create a new Domain (which is equivalent to an Elastic Search Cluster). The configuration options are:

**Deployment type**
 * For this guide the recommended option is 'Development and testing' as this will create a single node in one availability zone. For larger instances you may want to use a production type. 

**Elasticsearch version**
 * Select the latest version of ElasticSearch. At the time of writing the latest version is `7.7`.

**Elasticsearch domain name**
 * This is the name of your ElasticSearch service. I used `elastic-sas` but it should be a name that would distinguish the SAS use of ElasticSearch as apposed to other services that might need an ElasticSearch cluster. 

**Data nodes**
 * This is where you could scale your ElasticSearch interface and have multiple nodes. AWS recommends a minimum of 3 nodes.
 * For this guide we will go for the cheapest option which is: 
   * Instance type: **t2.small.elasticsearch**
   * Number of nodes: 1

**Data nodes storage**
This is the data storage for your ElasticSearch instance. Annotations with SAS are generally very small so the defaults should be fine here:
 * **Data nodes storage type**: EBS
 * **EBS volume type**: General Purpose (SSD)
 * **EBS storage size per node**: 10 - this is 10GB of storage

**Dedicated master nodes**
As we only have 1 node in our cluster (see **Data nodes** above) it is not possible to set a dedicated master node so do not enable this. 

**Network configuration**
To allow you to view the ElasticSearch index it is advisable to give it Public Access but restrict who can access the instance. To do this select **Public Access** rather than VPC in Network configuration.

**Fine-grained access control**
As we are using a small instance, Fine grained access control isn't available. 

**Amazon Cognito authentication**
For this guide we will be creating an IAM role rather than using Cognito. Do not enable Amazon Cognito authentication if you are following this guide. 

**Access Policy**
To allow SAS access to the ElasticSearch cluster we need to add a custom access policy. This will be fully populated after the ElasticSearch instance has been created but for now enter the user's ARN that you created here and set the first drop down to `IAM ARN`. The third drop down should be `Allow`.


Making the following changes:
 * **USER_ARN**: You should find this on the summary screen of the IAM user you created at the start of the Storage part of this guide.
 * **ELASTIC_SEARCH_ARN**: You should find this on the ElasticSearch information screen for your domain. It will be named **Domain ARN**.
 * **DEV_SERVER_IP**: To allow you to view data in ElasticSearch add your IP address so that you can view ElasticSearch and Kabana without using the IAM login. 

**Encryption**

Check **Require HTTPS for all traffic to the domain**. Do not select **Node-to-node encryption** or **Enable encryption of data at rest**. There is no need to fill in any of the **Optional Elasticsearch cluster settings**.

Now click `Confirm` to create your ElasticSearch instance. To aid development you may want to access ElasticSearch and Kabana from your development machine. If this is the case then select your domain and in the `Actions` drop down select **Modify access policy)**. 

Enter the following policy JSON:

```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "USER_ARN"
      },
      "Action": "es:*",
      "Resource": "ELASTIC_SEARCH_ARN"
    },
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": "es:ESHttp*",
      "Resource": "ELASTIC_SEARCH_ARN",
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": "DEV_SERVER_IP"
        }
      }
    }
  ]
}
```

