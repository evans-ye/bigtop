/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.bigtop.itest.hadoop.hdfs;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.bigtop.itest.JarContent;
import org.apache.hadoop.conf.Configuration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestWebHDFS {

  private static Shell sh = new Shell("/bin/bash -s");
  private static Shell shHDFS = new Shell("/bin/bash");
  private static final String HADOOP_HOME = System.getenv('HADOOP_HOME');
  private static final String HADOOP_CONF_DIR = System.getenv('HADOOP_CONF_DIR');
  private static final String USERNAME = System.getProperty("user.name");
  private static final String WEBHDFSURL = System.getenv('WEBHDFSURL');
  private static String date = sh.exec("date").getOut().get(0).
                               replaceAll("\\s","").replaceAll(":","");
  private static String testWebHDFSInput = "testWebHDFSInput$date";
  private static String testWebHDFStput = "testWebHDFSOutput$date";
  private static String namenode;
  private static Configuration conf;
  private CommonFunctions scripts = new CommonFunctions();
  /*
   * To run the below tests, please set dfs.webhdfs.enabled to true.
   * Else these tests will fail.
   */
  @BeforeClass
  public static void setUp() {
    // unpack resource
    JarContent.unpackJarContainer(TestWebHDFS.class, "." , null);

    conf = new Configuration();
    namenode = conf.get("fs.defaultFS");
    if (namenode == null) {
      namenode = conf.get("fs.default.name");
    }
    assertTrue("Could not find namenode", namenode != null);

    // prepare the test directories
    sh.exec("hdfs dfs -test -d /user/$USERNAME/webhdfs");
    if (sh.getRet() == 0)
    {
      sh.exec("hdfs dfs -rm -r /user/$USERNAME/webhdfs");
      assertTrue("Able to clean directory?", sh.getRet() == 0);
    }
    sh.exec("hdfs dfs -mkdir -p /user/$USERNAME/webhdfs");
    assertTrue("Able to clear directory?", sh.getRet() == 0);
  }

  @AfterClass
  public static void tearDown() {
    // deletion of test folder
    sh.exec("hadoop fs -test -e /user/$USERNAME/webhdfs");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash /user/$USERNAME/webhdfs");
      assertTrue("Deletion of previous webhdfs folder from HDFS failed",
          sh.getRet() == 0);
    }
    sh.exec("hadoop fs -test -e /user/$USERNAME/webhdfs_test");
    if (sh.getRet() == 0) {
      sh.exec("hadoop fs -rmr -skipTrash /user/$USERNAME/webhdfs_test");
      assertTrue("Deletion of previous webhdfs_test folder from HDFS failed",
          sh.getRet() == 0);
    }

  }

  @Test
  public void testMkdir() {
    println("testMkdir");
    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs?"+
            "user.name=$USERNAME&op=MKDIRS\"");
    assertTrue("could create directory using webhdfs", sh.getRet() == 0);
    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs?"+
            "user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could create directory using webhdfs", sh.getRet() == 0);
    List out_msgs = sh.getOut();
    Iterator out_iter = out_msgs.iterator();
    Boolean success_1 =false;
    String OUTMSG = "HTTP/1.1 200 OK";
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("{\"FileStatuses\":{\"FileStatus\":[]}}")) {
        success_1 =true;
      }
    }
    assertEquals("webhdfs directory is not created on hdfs ",true,success_1);
  }

  @Test
  public void testCreatWrite() {
    println("testCreatWrite");
    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs/"+
            "webtest_1.txt?user.name=$USERNAME&op=CREATE\"");
    assertTrue("could create file using webhdfs", sh.getRet() == 0);
    List out_msgs = sh.getOut();
    Iterator out_iter = out_msgs.iterator();
    Boolean success_1 =false;
    String Write_url;
    String OUTMSG = "HTTP/1.1 307 Temporary Redirect";
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains(OUTMSG)) {
        success_1 =true;
      }
      if (next_val.contains("Location: http://")) {
        Write_url = next_val.replaceAll("Location: ","");
      }
    }
    assertEquals("webhdfs did not create a url for file creationg on hdfs",
                 true,success_1);

    sh.exec("curl -i -X PUT -T test_data/test_1.txt \"${Write_url}\"");
    assertTrue("could not write to the file using webhdfs", sh.getRet() == 0);
    assertTrue("webhdfs did not write to the url on hdfs ",
                 scripts.lookForGivenString(sh.getOut(), "Created")==true);

    sh.exec("hdfs dfs -get /user/$USERNAME/webhdfs/webtest_1.txt " + 
            "webtest_1.txt");
    assertTrue("could not copy the file from hdfs to local", sh.getRet() == 0);
    sh.exec("diff test_data/test_1.txt webtest_1.txt");
    assertTrue("file written to hdfs and file copied from hdfs are differ",
               sh.getRet() == 0);
    sh.exec("rm -f webtest_1.txt");
  }

  @Test
  public void testAppend() {
    println("testAppend");

    sh.exec("curl -i -X POST \"${WEBHDFSURL}/user/${USERNAME}/webhdfs/"+
            "webtest_1.txt?user.name=$USERNAME&op=APPEND\"");
    assertTrue("could not append to a file using webhdfs", sh.getRet() == 0);
    List out_msgs = sh.getOut();
    Iterator out_iter = out_msgs.iterator();
    Boolean success_1 =false;
    String Write_url;
    String OUTMSG = "HTTP/1.1 307 Temporary Redirect";
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains(OUTMSG)) {
        success_1 =true;
      }
      if (next_val.contains("Location: http://")) {
        Write_url = next_val.replaceAll("Location: ","");
      }
    }
    assertEquals("webhdfs did not create a url for file creationg on hdfs",
                 true,success_1);

    sh.exec("curl -i -X POST -T test_data/test_2.txt \"${Write_url}\"");
    assertTrue("could not append to the file using webhdfs", sh.getRet() == 0);

    assertTrue("webhdfs did not append to the url on hdfs",
               scripts.lookForGivenString(sh.getOut(),"HTTP/1.1 200 OK")==true);

    sh.exec("hdfs dfs -get /user/$USERNAME/webhdfs/webtest_1.txt "+
            "webtest_1.txt");
    assertTrue("could not copy the file from hdfs to local", sh.getRet() == 0);
    sh.exec("cat test_data/test_1.txt test_data/test_2.txt > test_1_2.txt");
    assertTrue("could not cat two files on hdfs", sh.getRet() == 0);
    sh.exec("diff test_1_2.txt webtest_1.txt");
    assertTrue("file written to hdfs and file copied from hdfs are differ",
               sh.getRet() == 0);
    sh.exec("rm -f webtest_1.txt test_1_2.txt");
  }

  @Test
  public void testOpen() {
    println("testOpen");
    /*
     * create a temp file
     */
    sh.exec("echo \"Hi\" > tempOpen");
    sh.exec("hdfs dfs -put tempOpen /user/${USERNAME}/webhdfs/tempOpen");
    assertTrue("Able to upload file to hdfs?", sh.getRet() == 0);

    sh.exec("curl -L \"${WEBHDFSURL}/user/${USERNAME}/webhdfs/tempOpen?"+
            "user.name=$USERNAME&op=OPEN\" > test_1_2_read.txt");
    assertTrue("could not open a file using webhdfs", sh.getRet() == 0);

    sh.exec("diff tempOpen test_1_2_read.txt");
    assertTrue("file read from hdfs and file written to hdfs differ",
               sh.getRet() == 0);

    sh.exec("rm -f test_1_2_read.txt tempOpen");
    sh.exec("hdfs dfs -rm -r /user/${USERNAME}/tempOpen");
  }

  @Test
  public void testRename() {
    println("testRename");
    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs/"+
            "webtest_1.txt?user.name=$USERNAME&op=RENAME&destination="+
            "/user/$USERNAME/webhdfs/webtest_2.txt\"");
    assertTrue("could not rename file using webhdfs", sh.getRet() == 0);

    String OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("webhdfs did not rename the file opening on hdfs",
               scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);

    sh.exec("hdfs dfs -ls /user/$USERNAME/webhdfs");
    assertTrue("file read from hdfs and file written to hdfs differ",
               sh.getRet() == 0);

    Boolean success_1 =false;
    Boolean success_2 =false;
    String file1 = "/user/$USERNAME/webhdfs/webtest_2.txt";
    String file2 = "/user/$USERNAME/webhdfs/webtest_1.txt";
    success_1 = scripts.lookForGivenString(sh.getOut(), file1);
    success_2 = scripts.lookForGivenString(sh.getOut(), file2);
    assertTrue("file did not get renamed on hdfs ", success_1== true && 
               success_2== false);

    // rename folder
    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs?"+
            "user.name=$USERNAME&op=RENAME&destination=/user/$USERNAME/"+
            "webhdfs_test\"");
    assertTrue("could not rename folder using webhdfs", sh.getRet() == 0);

    OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("webhdfs did not rename the folder on hdfs",
               scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);

    assertEquals("webhdfs did not rename the folder on hdfs ",true,success_1);

    sh.exec("hdfs dfs -ls /user/$USERNAME/webhdfs");
    assertTrue("webhdfs folder still present on hdfs", sh.getRet() == 1);

    sh.exec("hdfs dfs -ls /user/$USERNAME/webhdfs_test");
    assertTrue("folder did not get renamed", sh.getRet() == 0);

    OUTMSG = "/user/$USERNAME/webhdfs_test";
    assertTrue("webhdfs did not rename the folder on hdfs",
               scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);
  }

  @Test
  public void testGetFileStatus() {
    println("testtGetFileStatus");
    /*
     * First upload a file to hdfs
     */
    createTempFile("/user/${USERNAME}/webhdfs_test", "webtest_2.txt");

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=GETFILESTATUS\"");
    assertTrue("could not get file status using webhdfs", sh.getRet() == 0);
    List out_msgs = sh.getOut();
    Iterator out_iter = out_msgs.iterator();
    Boolean success_1 =false;
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("\"owner\":\""+USERNAME+"\"") && 
                           next_val.contains("\"permission\":\"644\"") &&
                           next_val.contains("\"type\":\"FILE\"")) {
        success_1 =true;
      }
    }
    assertEquals("getfilestatus is not liisting proper values",true,success_1);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?user.name="+
            "$USERNAME&op=GETFILESTATUS\"");
    assertTrue("could not get file status using webhdfs", sh.getRet() == 0);
    out_msgs = sh.getOut();
    out_iter = out_msgs.iterator();
    success_1 =false;
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("\"owner\":\""+USERNAME+"\"") && 
          next_val.contains("\"permission\":\"755\"") && 
          next_val.contains("\"type\":\"DIRECTORY\"")) {
        success_1 =true;
      }
    }
    assertEquals("getfilestatus is not liisting proper values",true,success_1);
  }

  @Test
  public void testGetContentSummary() {
    println("testtGetContentSummary");
    /*
     * First upload a file to hdfs
     */
    createTempFile("/user/${USERNAME}/webhdfs_test", "webtest_2.txt");

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=GETCONTENTSUMMARY\"");
    assertTrue("could not get summary for a file using webhdfs",
               sh.getRet() == 0);
    List out_msgs = sh.getOut();
    Iterator out_iter = out_msgs.iterator();
    Boolean success_1 =false;
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      println(next_val);
      if (next_val.contains("\"directoryCount\":0") &&
          next_val.contains("\"fileCount\":1") && 
          next_val.contains("\"length\":0"))  {
        success_1 =true;
      }
    }
    assertEquals("getcontentsummary is not listing proper values for a file",
                 true,success_1);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=GETCONTENTSUMMARY\"");
    assertTrue("could not get summary for a folder using webhdfs",
               sh.getRet() == 0);
    out_msgs = sh.getOut();
    out_iter = out_msgs.iterator();
    success_1 =false;
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      //   println(next_val);
      if (next_val.contains("\"directoryCount\":1") && 
          next_val.contains("\"fileCount\":1") && 
          next_val.contains("\"length\":0")) {
        success_1 =true;
      }
    }
    assertEquals("getcontentsummary is not listing proper values for a folder",
                 true,success_1);
  }

  @Test
  public void testGetFileChecksum() {
    println("testtGetFileChecksum");
    /*
     * First upload a file to hdfs
     */
    createTempFile("/user/${USERNAME}/webhdfs_test", "webtest_2.txt");

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=GETFILECHECKSUM\"");
    assertTrue("could not get checksum for a file using webhdfs",
               sh.getRet() == 0);

    Boolean success_1 =false;
    success_1 = scripts.lookForGivenString(sh.getOut(),"\"FileChecksum\":") &&
                scripts.lookForGivenString(sh.getOut(),"\"length\":28");
    assertTrue("getchecksum failed for file", success_1 == true);
            
    // now delete the created temp file
    sh.exec("hdfs dfs -rm -r /user/${USERNAME}/webhdfs_test/webtest_2.txt")
    assertTrue("Failed to clean test file?", sh.getRet() == 0);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=GETFILECHECKSUM\"");
    assertTrue("could not get checksum for a folder using webhdfs",
               sh.getRet() == 0);

    assertTrue("getchecksum failed for folder?",
                scripts.lookForGivenString(sh.getOut(),
                        "\"message\":\"Path is not a file") == true);
  }

  @Test
  public void testGetHomeDir() {
    println("testGetHomeDir");
    sh.exec("curl -i \"${WEBHDFSURL}?user.name=$USERNAME"+
            "&op=GETHOMEDIRECTORY\"");
    assertTrue("could not get home directory using webhdfs", sh.getRet() == 0);
    String OUTMSG = "\\/user\\/"+USERNAME+"";
    assertTrue("home directory does not match the expected",
               scripts.lookForGivenString(sh.getOut(), OUTMSG) == true);
  }

  @Test
  public void testSetReplication() {
    println("testSetReplication");

    createTempFile("/user/${USERNAME}/webhdfs_test", "webtest_2.txt");

    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=SETREPLICATION&"+
            "replication=2\"");
    assertTrue("could not set replication for a file using webhdfs",
               sh.getRet() == 0);

    String OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not recieved",
               scripts.lookForGivenString(sh.getOut(), OUTMSG) == true);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a file using webhdfs",
               sh.getRet() == 0);

    String msg1 = "\"owner\":\""+USERNAME+"\"";
    String msg2 = "\"permission\":\"644\"";
    String msg3 = "\"replication\":2";
    boolean success_1 = scripts.lookForGivenString(sh.getOut(), msg1) &&
                        scripts.lookForGivenString(sh.getOut(), msg2) &&
                        scripts.lookForGivenString(sh.getOut(), msg3);
    assertEquals("replication factor not set proeprly",true,success_1);

    // folder
    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=SETREPLICATION&replication=2\"");
    assertTrue("could not set replication for a folder using webhdfs",
               sh.getRet() == 0);

    OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not recieved for directory",
               scripts.lookForGivenString(sh.getOut(), OUTMSG) == true);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a directory using webhdfs",
               sh.getRet() == 0);

    success_1 = scripts.lookForGivenString(sh.getOut(), msg1) &&
                scripts.lookForGivenString(sh.getOut(), msg2) &&
                scripts.lookForGivenString(sh.getOut(), msg3);
    assertEquals("replication factor not set proeprly for a directory",
                 true,success_1);
  }

  @Test
  public void testSetPermissions() {
    println("testSetPermissions");
    /*
     * First upload a file to hdfs
     */
    createTempFile("/user/${USERNAME}/webhdfs_test", "webtest_2.txt");

    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=SETPERMISSION&"+
            "permission=600\"");
    assertTrue("could not set permissions for a file using webhdfs",
               sh.getRet() == 0);

    Boolean success_1 =false;
    String OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not recieved",
               scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a file using webhdfs",
               sh.getRet() == 0);
    List out_msgs = sh.getOut();
    Iterator out_iter = out_msgs.iterator();
    success_1 =false;
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("\"owner\":\""+USERNAME+"\"") &&
         next_val.contains("\"permission\":\"600\"")) {
        success_1 =true;
      }
    }
    assertEquals("permissions not set properly",true,success_1);

    // test for folder
    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=SETPERMISSION&replication=2\"");
    assertTrue("could not set permissions for a folder using webhdfs",
               sh.getRet() == 0);

    success_1 =false;
    OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not recieved  for directory",
               scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a directory using webhdfs",
               sh.getRet() == 0);

    out_msgs = sh.getOut();
    out_iter = out_msgs.iterator();
    success_1 =false;
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      //   println(next_val);
      if( next_val.contains("\"owner\":\""+USERNAME+"\"") &&
          next_val.contains("\"permission\":\"600\"")) {
        success_1 =true;
      }
    }
    assertEquals("permissions not set properly for a directory",
                  true,success_1);
  }

  @Test
  public void testSetTimes() {
    println("testSetTimes");
    /*
     * First upload a file to hdfs
     */
    createTempFile("/user/${USERNAME}/webhdfs_test", "webtest_2.txt");

    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=SETTIMES&"+
            "modificationtime=1319575753923&accesstime=1369575753923\"");
    assertTrue("could not set times for a file using webhdfs",
               sh.getRet() == 0);

    Boolean success_1 =false;
    String OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not recieved",
               scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a file using webhdfs",
               sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Iterator out_iter = out_msgs.iterator();
    success_1 =false;
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("\"owner\":\""+USERNAME+"\"") &&
          next_val.contains("\"accessTime\":1369575753923") &&
          next_val.contains("\"modificationTime\":1319575753923")) {
        success_1 =true;
      }
    }
    assertEquals("times not set properly",true,success_1);

    // folder
    sh.exec("curl -i -X PUT \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=SETTIMES&modificationtime=1319575753923&"+
            "accesstime=1369575753923\"");
    assertTrue("could not set times for a folder using webhdfs",
               sh.getRet() == 0);

    OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not recieved for directory",
               scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);

    sh.exec("curl -i \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=LISTSTATUS\"");
    assertTrue("could not list status for a directory using webhdfs",
               sh.getRet() == 0);
    out_msgs = sh.getOut();
    out_iter = out_msgs.iterator();
    success_1 =false;
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("\"owner\":\""+USERNAME+"\"") &&
          next_val.contains("\"accessTime\":1369575753923") &&
          next_val.contains("\"modificationTime\":1319575753923")) {
        success_1 =true;
      }
    }
    assertEquals("times not set properly for a directory",true,success_1);
  }

  @Test
  public void testDelte() {
    println("testDelete");
    sh.exec("curl -i -X DELETE \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test/"+
            "webtest_2.txt?user.name=$USERNAME&op=DELETE\"");
    assertTrue("could not delete a file using webhdfs", sh.getRet() == 0);

    Boolean success_1 =false;
    String OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not recieved",
               scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);

    sh.exec("hdfs dfs -ls /user/${USERNAME}/webhdfs_test/webtest_2.txt");
    assertTrue("webtest_2.txt should not be present", sh.getRet() == 1);

    // folder
    sh.exec("curl -i -X DELETE \"${WEBHDFSURL}/user/${USERNAME}/webhdfs_test?"+
            "user.name=$USERNAME&op=DELETE\"");
    assertTrue("could not delete a directory using webhdfs", sh.getRet() == 0);

    success_1 =false;
    OUTMSG = "HTTP/1.1 200 OK";
    assertTrue("expected HTTP status of 200 not recieved for directory",
                scripts.lookForGivenString(sh.getOut(),OUTMSG)==true);

    sh.exec("hdfs dfs -ls /user/${USERNAME}/webhdfs_test");
    assertTrue("webhdfs_tests directory still present on hdfs",
               sh.getRet() == 1);
  }

  public void createTempFile(String parentDir, String fileName)
  {
    /*
     * create parent directory first
     */
    sh.exec("hdfs dfs -test -d " + parentDir);
    if (sh.getRet() == 0) {
      sh.exec("hdfs dfs -rm -r " + parentDir);
    }
    sh.exec("hdfs dfs -mkdir -p " + parentDir)
    /*
     * First upload a file to hdfs
     */
    sh.exec("hdfs dfs -test -e $parentDir/$fileName")
    // if the file already present then delete it
    if (sh.getRet() == 0 ) {
      sh.exec("hdfs dfs -rm -r $parentDir/$fileName")
      assertTrue("Failed to clean test file $parentDir/$fileName?",
                 sh.getRet() == 0);
    }
    sh.exec("hdfs dfs -touchz $parentDir/$fileName");
    assertTrue("Failed to create test file $parentDir/$fileName?",
               sh.getRet() == 0);
  }
}
