/**
* Copyright 2016 ZuInnoTe (Jörn Franke) <zuinnote@gmail.com>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
**/
package org.zuinnote.spark.bitcoin.block

import scala.collection.JavaConversions._


import org.apache.spark.sql.sources.{BaseRelation,TableScan}
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.types.ArrayType
import org.apache.spark.sql.types.ByteType
import org.apache.spark.sql.types.BinaryType
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.sql.types.LongType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.SQLContext

import org.apache.spark.sql._
import org.apache.spark.rdd.RDD

import org.apache.hadoop.conf._
import org.apache.hadoop.mapred._



import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log


import org.zuinnote.hadoop.bitcoin.format.common._
import org.zuinnote.hadoop.bitcoin.format.mapreduce._
import org.zuinnote.spark.bitcoin.util.BitcoinBlockFile

/**
* Author: Jörn Franke <zuinnote@gmail.com>
*
*/

/**
* Defines the schema of a BitcoinBlock for Spark SQL
*
*/

case class BitcoinBlockRelation(location: String,maxBlockSize: Integer = AbstractBitcoinRecordReader.DEFAULT_MAXSIZE_BITCOINBLOCK,magic: String = AbstractBitcoinRecordReader.DEFAULT_MAGIC,useDirectBuffer: Boolean = AbstractBitcoinRecordReader.DEFAULT_USEDIRECTBUFFER,isSplitable: Boolean = AbstractBitcoinFileInputFormat.DEFAULT_ISSPLITABLE)
(@transient val sqlContext: SQLContext)
  extends BaseRelation with TableScan
       with Serializable {
  val LOG = LogFactory.getLog(BitcoinBlockRelation.getClass);

  override def schema: StructType = {

      return StructType(Seq(StructField("blockSize", IntegerType, false),
                            StructField("magicNo", BinaryType, false),
				StructField("version", IntegerType, false),
				StructField("time", IntegerType, false),
				StructField("bits", BinaryType, false),
				StructField("nonce", IntegerType, false),
				StructField("transactionCounter", LongType, false),
				StructField("hashPrevBlock", BinaryType, false),
				StructField("hashMerkleRoot", BinaryType, false),
				StructField("transactions",ArrayType(StructType(Seq
					(StructField("version", IntegerType, false),
          StructField("marker", ByteType, false),
          StructField("flag", ByteType, false),
					StructField("inCounter",BinaryType, false),
					StructField("outCounter", BinaryType, false),
					StructField("listOfInputs",ArrayType(StructType(Seq(
						StructField("prevTransactionHash",BinaryType,false),
						StructField("previousTxOutIndex",LongType,false),
						StructField("txInScriptLength",BinaryType,false),
						StructField("txInScript",BinaryType,false),
						StructField("seqNo",LongType,false)))), false),
					StructField("listOfOutputs",ArrayType(StructType(Seq(
						StructField("value",LongType,false),
						StructField("txOutScriptLength",BinaryType,false),
						StructField("txOutScript",BinaryType,false)))), false),
            StructField("listOfScriptWitnessItem",ArrayType(StructType(Seq(
      						StructField("stackItemCounter",BinaryType,false),
                  StructField("scriptWitnessList",ArrayType(StructType(Seq(
                    StructField("witnessScriptLength",BinaryType,false),
                    StructField("witnessScript",BinaryType,false)
                  )), false)
      						))), false)),
            	StructField("lockTime", IntegerType, false)))
			))))
    }



    /**
     * Used by Spark to fetch Bitcoin blocks according to the schema specified above from files.
     *
     *
     * returns BitcoinBlocks as rows
    **/
    override def buildScan: RDD[Row] = {
	// create hadoopConf
	val hadoopConf = new Configuration()
 	hadoopConf.set(AbstractBitcoinRecordReader.CONF_MAXBLOCKSIZE,String.valueOf(maxBlockSize))
 	hadoopConf.set(AbstractBitcoinRecordReader.CONF_FILTERMAGIC,magic)
 	hadoopConf.set(AbstractBitcoinRecordReader.CONF_USEDIRECTBUFFER,String.valueOf(useDirectBuffer))
 	hadoopConf.set(AbstractBitcoinFileInputFormat.CONF_ISSPLITABLE, String.valueOf(isSplitable))
        // read BitcoinBlock
	val bitcoinBlockRDD = BitcoinBlockFile.load(sqlContext, location, hadoopConf)
        // map to schema
	val schemaFields = schema.fields
	val rowArray = new Array[Any](schemaFields.length)
        bitcoinBlockRDD.flatMap(hadoopKeyValueTuple => {
		// map the BitcoinBlock data structure to a Spark SQL schema
		rowArray(0) = hadoopKeyValueTuple._2.getBlockSize
		rowArray(1) = hadoopKeyValueTuple._2.getMagicNo
		rowArray(2) = hadoopKeyValueTuple._2.getVersion
		rowArray(3) = hadoopKeyValueTuple._2.getTime
		rowArray(4) = hadoopKeyValueTuple._2.getBits
		rowArray(5) = hadoopKeyValueTuple._2.getNonce
		rowArray(6) = hadoopKeyValueTuple._2.getTransactionCounter
		rowArray(7) = hadoopKeyValueTuple._2.getHashPrevBlock
		rowArray(8) = hadoopKeyValueTuple._2.getHashMerkleRoot
		// map transactions
		var transactionArray=new Array[Any](hadoopKeyValueTuple._2.getTransactions().size())
		var i=0
		for (currentTransaction <- hadoopKeyValueTuple._2.getTransactions()) {
			val currentTransactionStructArray = new Array[Any](9)
			currentTransactionStructArray(0)=currentTransaction.getVersion
      currentTransactionStructArray(1)=currentTransaction.getMarker
      currentTransactionStructArray(2)=currentTransaction.getFlag
			currentTransactionStructArray(3)=currentTransaction.getInCounter
			currentTransactionStructArray(4)=currentTransaction.getOutCounter
			val currentTransactionListOfInputs = new Array[Any](currentTransaction.getListOfInputs().size())
			// map inputs
			var j=0
			for (currentTransactionInput <-currentTransaction.getListOfInputs) {
				val currentTransactionInputStructArray = new Array[Any](5)
				currentTransactionInputStructArray(0)=currentTransactionInput.getPrevTransactionHash
				currentTransactionInputStructArray(1)=currentTransactionInput.getPreviousTxOutIndex
				currentTransactionInputStructArray(2)=currentTransactionInput.getTxInScriptLength
				currentTransactionInputStructArray(3)=currentTransactionInput.getTxInScript
				currentTransactionInputStructArray(4)=currentTransactionInput.getSeqNo
				currentTransactionListOfInputs(j)=Row.fromSeq(currentTransactionInputStructArray)
				j+=1
			}
			currentTransactionStructArray(5)=currentTransactionListOfInputs
			val currentTransactionListOfOutputs = new Array[Any](currentTransaction.getListOfOutputs().size())
			// map outputs
			j=0;
			for (currentTransactionOutput <-currentTransaction.getListOfOutputs) {
				val currentTransactionOutputStructArray = new Array[Any](3)
				currentTransactionOutputStructArray(0) = currentTransactionOutput.getValue
				currentTransactionOutputStructArray(1) = currentTransactionOutput.getTxOutScriptLength
				currentTransactionOutputStructArray(2) = currentTransactionOutput.getTxOutScript
				currentTransactionListOfOutputs(j)=Row.fromSeq(currentTransactionOutputStructArray)
				j+=1
			}
			currentTransactionStructArray(6)=currentTransactionListOfOutputs


      // map scriptWitness
      val currentTransactionListOfScriptWitnessItem = new Array[Any](currentTransaction.getBitcoinScriptWitness().size())
      j=0;
      for (currentTransactionScriptWitnessItem <-currentTransaction.getBitcoinScriptWitness) {
        val currentTransactionScriptWitnessStructArray = new Array[Any](2)
        currentTransactionScriptWitnessStructArray(0) = currentTransactionScriptWitnessItem.getStackItemCounter
        val currentScriptWitnessListStructArray = new Array[Any](currentTransactionScriptWitnessItem.getScriptWitnessList().size())
        var k=0;
        for (currentScriptWitness <- currentTransactionScriptWitnessItem.getScriptWitnessList) {
              val currentScriptWitnessStructArray = new Array[Any](2)
              currentScriptWitnessStructArray(0)= currentScriptWitness.getWitnessScriptLength
              currentScriptWitnessStructArray(1)= currentScriptWitness.getWitnessScript
              currentScriptWitnessListStructArray(k)=Row.fromSeq(currentScriptWitnessStructArray)
             k+=1
        }
        currentTransactionScriptWitnessStructArray(1) = currentScriptWitnessListStructArray
        currentTransactionListOfScriptWitnessItem(j)=Row.fromSeq(currentTransactionScriptWitnessStructArray)
        j+=1
      }
      currentTransactionStructArray(7)=currentTransactionListOfScriptWitnessItem

      // locktime
			currentTransactionStructArray(8)=currentTransaction.getLockTime
			transactionArray(i)=Row.fromSeq(currentTransactionStructArray)

			i+=1
		}
		rowArray(9) = transactionArray
	 	// add row representing one Bitcoin Block
          	Some(Row.fromSeq(rowArray))
		}
        )

     }


}
