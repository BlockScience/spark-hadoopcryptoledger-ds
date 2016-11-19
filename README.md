# spark-hadoopcryptoledger-ds
A Spark datasource for the [HadoopCryptoLedger](https://github.com/ZuInnoTe/hadoopcryptoledger/wiki) library. This Spark datasource assumes at least Spark 1.5.
Currently this datasource supports the following formats of the HadoopCryptoLedger library:
* Bitcoin Blockchain
 * Bitcoin Block Datasource format: org.zuinnote.spark.bitcoin.block
 * Bitcoin Transaction Datasource format: org.zuinnote.spark.bitcoin.transaction
 * Bitcoin TransactionElement Datasource format: org.zuinnote.spark.bitcoin.transactionelement

This datasource will be soon available on https://spark-packages.org/ and Maven Central.

# Options
The following options are mapped to the following options of the HadoopCryptoLedger library ([Explanation](https://github.com/ZuInnoTe/hadoopcryptoledger/wiki/Hadoop-File-Format#configure)):
* "magic" is mapped to "hadoopcryptoledger.bitcoinblockinputformat.filter.magic"
* "maxblockSize" is mapped to "hadoopcryptoledger.bitcoinblockinputformat.maxblocksize"
* "useDirectBuffer" is mapped to "hadoopcryptoledeger.bitcoinblockinputformat.usedirectbuffer"
* "isSplitable" is mapped to "hadoopcryptoledeger.bitcoinblockinputformat.issplitable"


# Dependency
## Scala 2.10

groupId: com.github.zuinnote

artifactId: spark-hadoopcryptoledger-ds_2.10

version: 1.0.2

## Scala 2.11
 
groupId: com.github.zuinnote

artifactId: spark-hadoopcryptoledger-ds_2.11

version: 1.0.2


# Develop
The following sections describe some example code. 
## Scala
 This example loads Bitcoin Blockhain data from the folder "/home/user/bitcoin/input" using the BitcoinBlock representation (format).
 ```
val sqlContext = new SQLContext(sc)
val df = sqlContext.read
    .format("org.zuinnote.spark.bitcoin.block")
    .option("magic", "F9BEB4D9")
    .load("/home/user/bitcoin/input")
```
 The HadoopCryptoLedger library provides an example for scala using the data source library: https://github.com/ZuInnoTe/hadoopcryptoledger/tree/master/examples/scala-spark-datasource-bitcoinblock
## Java
 This example loads Bitcoin Blockhain data from the folder "/home/user/bitcoin/input" using the BitcoinBlock representation (format).
 ```
import org.apache.spark.sql.SQLContext

SQLContext sqlContext = new SQLContext(sc);
DataFrame df = sqlContext.read()
    .format("org.zuinnote.spark.bitcoin.block")
    .option("magic", "F9BEB4D9")
    .load("/home/user/bitcoin/input");
```
## R
 This example loads Bitcoin Blockhain data from the folder "/home/user/bitcoin/input" using the BitcoinBlock representation (format).
```
library(SparkR)

Sys.setenv('SPARKR_SUBMIT_ARGS'='"--packages" "com.databricks:spark-hadoopcrytoledger-ds_2.10:1.0.2" "sparkr-shell"')
sqlContext <- sparkRSQL.init(sc)

df <- read.df(sqlContext, "/home/user/bitcoin/input", source = "org.zuinnote.spark.bitcoin.block", magic = "F9BEB4D9")
 ```
## SQL
The following statement creates a table that contains Bitcoin Blockchain data in the folder /home/user/bitcoin/input
```
CREATE TABLE BitcoinBlockchain
USING  org.zuinnote.spark.bitcoin.block
OPTIONS (path "/home/user/bitcoin/input", magic "F9BEB4D9")
```
