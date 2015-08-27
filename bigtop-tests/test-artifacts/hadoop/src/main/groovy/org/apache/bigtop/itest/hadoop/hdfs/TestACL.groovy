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

import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.*;
import org.junit.Test;
import org.apache.bigtop.itest.shell.Shell;
import org.apache.bigtop.itest.JarContent;
import org.apache.hadoop.conf.Configuration;
import static org.apache.bigtop.itest.LogErrorsUtils.logError;
import org.junit.runners.MethodSorters;

public class TestACL {

  /**
   * This class contains the tests to test the hdfs dfs -setfacl
   * command with different flags. To run the below tests we have
   * to make sure that the property dfs.namenode.acls.enabled is
   * set to true else all the below tests will fail.
   */

  private static Shell sh = new Shell("/bin/bash -s");
  private static final String USERNAME = System.getProperty("user.name");
  private static String date = sh.exec("date").getOut().get(0).replaceAll("\\s","").replaceAll(":","");
  private static String namenode = "";
  private static String testACLInputDir = "testACLInputDir" + date;
  private static String testACLInputs = "test_data_TestACL"
  private static String testACLOut = "testACLOut" + date;
  private static int repfactor = 2;

  @BeforeClass
  public static void setUp() {

    // unpack resource
    JarContent.unpackJarContainer(TestACL.class, "." , null);

    sh.exec("cp -r test_data test_data_TestACL");
    assertTrue("Could not copy data into test_data_TestACL", sh.getRet() == 0);

    // get namenode hostname from core-site.xml
    Configuration conf = new Configuration();
    namenode = conf.get("fs.defaultFS");
    if (namenode == null) {
      namenode = conf.get("fs.default.name");
    }
    assertTrue("Could not find namenode", namenode != null);

    sh.exec("hdfs dfs -test -d /user/$USERNAME/$testACLInputDir");
    if (sh.getRet() == 0) {
      println("hdfs dfs -rm -r -skipTrash /user/$USERNAME/$testACLInputDir")
      sh.exec("hdfs dfs -rm -r -skipTrash /user/$USERNAME/$testACLInputDir");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("hdfs dfs -mkdir -p /user/$USERNAME/$testACLInputDir");
    assertTrue("Could not create input directory on HDFS", sh.getRet() == 0);

    // copy input directory to hdfs
    sh.exec("hdfs dfs -put $testACLInputs /user/$USERNAME/$testACLInputDir");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);

    println("Running ACL:");
  }

  @AfterClass
  public static void tearDown() {
    sh.exec("hdfs dfs -test -d /user/$USERNAME/$testACLInputDir");
    if (sh.getRet() == 0) {
      println("hdfs dfs -rm -r -skipTrash /user/$USERNAME/$testACLInputDir")
      sh.exec("hdfs dfs -rm -r -skipTrash /user/$USERNAME/$testACLInputDir");
      assertTrue("Could not remove input directory", sh.getRet() == 0);
    }

    sh.exec("test -d $testACLInputs");
    if (sh.getRet() == 0) {
      sh.exec("rm -rf $testACLInputs");
      assertTrue("Could not remove output directory/file", sh.getRet() == 0);
    }
  }

  @Test
  public void testACLBasics() {
    println("TestACLBasics");
    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
	    "$testACLInputs/test_1.txt");
    assertTrue("Could not list ACL for a file", sh.getRet() == 0);
  }

  @Test
  public void testACLModifyRecursive() {
    println("TestACLModifyRecursive");
    sh.exec("hdfs dfs -setfacl -R -m user:hadoop:r-x,group::rw-,other::rw- " +
            "/user/$USERNAME/$testACLInputDir/$testACLInputs");
    assertTrue("Could not set ACL recursively for a folder", sh.getRet() == 0);

    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
	    "$testACLInputs/test_1.txt");
    assertTrue("Could not list ACL for a file", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Boolean success_1 = false;
    Boolean success_2 = false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:r-x")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("group::rw-")) {
        success_2 = true;
        continue;
      }
    }
    assertTrue("set ACL failed to apply to a folder recursively",
               success_1 == true && success_2 == true);
  }

  @Test
  public void testACLModifyFile() {
    println("TestACLModifyFile");
    sh.exec("hdfs dfs -setfacl -m user:hadoop:rw-,group::r--,other::r-- " +
            "/user/$USERNAME/$testACLInputDir/$testACLInputs/test_1.txt");
    assertTrue("Could not set ACL for a file", sh.getRet() == 0);

    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs/test_1.txt");
    assertTrue("Could not list ACL for a file", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Boolean success_1 = false;
    Boolean success_2 = false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:rw-")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("group::r--")) {
        success_2 = true;
        continue;
      }
    }
    assertTrue("set ACL failed to apply to a file",
               success_1 == true && success_2 == true);
  }

  @Test
  public void testACLModifyFolder() {
    println("TestACLModifyFolder");
    sh.exec("hdfs dfs -setfacl -m user:hadoop:r-x,group::r--,other::r-- " +
            "/user/$USERNAME/$testACLInputDir/$testACLInputs");
    assertTrue("Could not set ACL for a folder", sh.getRet() == 0);

    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs");
    assertTrue("Could not list ACL for a folder", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Boolean success_1 = false;
    Boolean success_2 = false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:r-x")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("group::r--")) {
        success_2 = true;
        continue;
      }
    }
    assertTrue("set ACL failed to apply to a folder",
               success_1 == true && success_2 == true);

    // checking for non recursive behaviour
    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs/test_2.txt");
    assertTrue("Could not list ACL for a folder", sh.getRet() == 0);

    out_msgs = sh.getOut();
    success_1 = false;
    success_2 = false;

    out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:r-x")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("group::r--")) {
         success_2 = true;
         continue;
       }
    }
    assertTrue("set ACL should have been non recursive",
               success_1 == false && success_2 == true);
  }

  @Test
  public void testACLRemoveSpecifed() {
    println("TestACLRemoveSpecifed");
    sh.exec("hdfs dfs -setfacl -x user:hadoop /user/$USERNAME/" +
            "$testACLInputDir/$testACLInputs/test_1.txt");
    assertTrue("Could not remove ACL for a file", sh.getRet() == 0);

    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs/test_1.txt");
    assertTrue("Could not list ACL for a file", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Boolean success_1 = false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:rw-")) {
        success_1 = true;
        break;
      }
    }
    assertTrue("remove ACL failed to apply to a file", success_1 == false);

    // apply to a folder
    sh.exec("hdfs dfs -setfacl -R -x user:hadoop /user/$USERNAME/" +
            "$testACLInputDir/$testACLInputs");
    assertTrue("Could not remove ACL for a file", sh.getRet() == 0);

    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs/test_2.txt");
    assertTrue("Could not list ACL for a file", sh.getRet() == 0);

    out_msgs = sh.getOut();
    success_1 = false;
    out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:rw-")) {
        success_1 = true;
        break;
      }
    }
    assertTrue("remove ACL failed to apply to a file", success_1 == false);
  }

  @Test
  public void testACLRemoveAllButBase() {
    println("TestACLRemoveAllButBase");
    sh.exec("hdfs dfs -setfacl -R -m user:hadoop:r-x,group::rw-,other::rw- " +
            "/user/$USERNAME/$testACLInputDir/$testACLInputs");
    assertTrue("Could not set ACL for a folder", sh.getRet() == 0);

    sh.exec("hdfs dfs -setfacl -b /user/$USERNAME/$testACLInputDir/"+
            "$testACLInputs/test_1.txt");
    assertTrue("Could not remove ACL for a file", sh.getRet() == 0);

    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs/test_1.txt");
    assertTrue("Could not list ACL for a file", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Boolean success_1 = false;
    Boolean success_2 = false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:rw-")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("group::rw-")) {
        success_2 = true;
        continue;
      }
    }
    assertTrue("remove all but base ACL failed to apply to a file",
               success_1 == false && success_2 == true);

    // apply to a folder
    sh.exec("hdfs dfs -setfacl -R -b /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs");
    assertTrue("Could not recursively remove ACL for a folder",
               sh.getRet() == 0);

    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs/test_2.txt");
    assertTrue("Could not list ACL for a file", sh.getRet() == 0);

    out_msgs = sh.getOut();
    success_1 = false;
    success_2 = false;
    out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:rw-")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("group::rw-")) {
        success_2 = true;
        continue;
      }
    }
    assertTrue("remove all but base ACL failed to apply to a folder",
               success_1 == false && success_2 == true);
  }

  @Test
  public void testACLSetNew() {
    println("TestACLSetNew");
    sh.exec("hdfs dfs -setfacl --set user::rwx,user:hadoop:rwx," +
            "group::r-x,other::r-x /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs/test_1.txt");
    assertTrue("Could not set new ACL for a file", sh.getRet() == 0);

    sh.exec("hdfs dfs -getfacl /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs/test_1.txt");
    assertTrue("Could not list ACL for a file", sh.getRet() == 0);

    List out_msgs = sh.getOut();
    Boolean success_1 = false;
    Boolean success_2 = false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      if (next_val.contains("user:hadoop:rwx")) {
        success_1 = true;
        continue;
      }
      if (next_val.contains("group::r-x")) {
        success_2 = true;
        continue;
      }
    }
    assertTrue("set new ACL failed to apply to a file",
               success_1 == true && success_2 == true);
  }

  @Test
  public void testACLSetNewParamMissing() {
    println("TestACLSetNewParamMissing");
    // first set the proper permission to the test folder
    sh.exec("hdfs dfs -setfacl --set user::rwx,user:hadoop:rw-,group::r-x," +
            "other::r-x /user/$USERNAME/$testACLInputDir/$testACLInputs");
    // now try to set acl without specifying any value for other section
    sh.exec("hdfs dfs -setfacl --set user::rw-,user:hadoop:rw-,group::r-- " +
            "/user/$USERNAME/$testACLInputDir/$testACLInputs/test_1.txt");
    assertTrue("Could not set new ACL for a file", sh.getRet() == 1);

    List out_msgs = sh.getErr();
    Boolean success_1 = false;
    Iterator out_iter = out_msgs.iterator();
    while (out_iter.hasNext()) {
      String next_val = out_iter.next();
      String expText = "setfacl: Invalid ACL: the user, " +
                       "group and other entries are required";
      if (next_val.contains(expText)) {
        success_1 = true;
        break;
      }
    }
    assertTrue("set new ACL executed with missing parameters",
               success_1 == true);
  }

  @Test
  public void testACLRemoveFolder() {
    println("TestACLRemoveFolder");

    // disable write permission to hadoop user
    sh.exec("hdfs dfs -setfacl --set user::rwx,user:hadoop:r--,group::rwx," +
            "other::rwx /user/$USERNAME/$testACLInputDir/$testACLInputs");
    // try to delete the folder using hadoop user
    Shell sh_root = new Shell("/bin/bash -s","hadoop");
    sh_root.exec("hdfs dfs -rm -r -skipTrash /user/$USERNAME/" +
                 "$testACLInputDir/$testACLInputs");
    assertTrue("Folder deleted without proper permissions",
               sh_root.getRet() == 1);

    // now provide requried permissions to enable delete operation for hadoop user
    sh.exec("hdfs dfs -setfacl --set user::rwx,user:hadoop:rwx,group::rwx," +
            "other::rwx /user/$USERNAME");
    assertTrue("Could not set permisions for folder: " + "/user/$USERNAME",
               sh.getRet() == 0);
    sh.exec("hdfs dfs -setfacl --set user::rwx,user:hadoop:rwx,group::rwx," +
            "other::rwx /user/$USERNAME/$testACLInputDir");
    assertTrue("Could not set permisions for folder: " +
               "/user/$USERNAME/$testACLInputDir", sh.getRet() == 0);

    sh.exec("hdfs dfs -setfacl --set user::rwx,user:hadoop:rwx,group::rwx," +
            "other::rwx /user/$USERNAME/$testACLInputDir/$testACLInputs");
    assertTrue("Could not set permisions for folder: " +
               "/user/$USERNAME/$testACLInputDir/$testACLInputs",
               sh.getRet() == 0);

    sh.exec("hdfs dfs -chmod -R 777 /user/$USERNAME/$testACLInputDir/" +
            "$testACLInputs");
    assertTrue("Could not set permisions using chmod for folder: " +
               "/user/$USERNAME/$testACLInputDir/$testACLInputs",
               sh.getRet() == 0);

    sh_root.exec("hdfs dfs -rm -r -skipTrash /user/$USERNAME/" +
                 "$testACLInputDir/$testACLInputs");
    assertTrue("Could not delete folder", sh_root.getRet() == 0);

    // revert back permissions
    sh.exec("hdfs dfs -setfacl --set user::rwx,user:hadoop:r--,group::r-x," +
            "other::r-x /user/$USERNAME")
    sh.exec("hdfs dfs -setfacl --set user::rwx,user:hadoop:r--,group::r-x," +
            "other::r-x /user/$USERNAME/$testACLInputDir")
    // copy test folder again to hdfs as these files will be used by other tests
    sh.exec("hdfs dfs -put $testACLInputs /user/$USERNAME/$testACLInputDir");
    assertTrue("Could not copy files to HDFS", sh.getRet() == 0);
  }
}
