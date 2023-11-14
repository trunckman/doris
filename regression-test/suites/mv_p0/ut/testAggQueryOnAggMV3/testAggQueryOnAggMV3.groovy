// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import org.codehaus.groovy.runtime.IOGroovyMethods

suite ("testAggQueryOnAggMV3") {
    sql """set enable_nereids_planner=true;"""
    sql """ DROP TABLE IF EXISTS emps; """

    sql """
            create table emps (
                time_col date, 
                empid int, 
                name varchar, 
                deptno int, 
                salary int, 
                commission int)
            partition by range (time_col) (partition p1 values less than MAXVALUE) distributed by hash(time_col) buckets 3 properties('replication_num' = '1');
        """

    sql """insert into emps values("2020-01-01",1,"a",1,1,1);"""
    sql """insert into emps values("2020-01-02",2,"b",2,2,2);"""
    sql """insert into emps values("2020-01-03",3,"c",3,3,10);"""
    sql """insert into emps values("2020-01-04",4,"d",21,4,4);"""



    createMV("create materialized view emps_mv as select deptno, commission, sum(salary) from emps group by deptno, commission;")


    explain {
        sql("select * from emps order by empid;")
        contains "(emps)"
    }
    qt_select_star "select * from emps order by empid;"


    explain {
        sql("select commission, sum(salary) from emps where commission * (deptno + commission) = 100 group by commission order by commission;")
        contains "(emps_mv)"
    }
    qt_select_mv "select commission, sum(salary) from emps where commission * (deptno + commission) = 100 group by commission order by commission;"

    explain {
        sql("select commission, sum(salary) from emps where commission = 100 group by commission order by commission;")
        contains "(emps_mv)"
    }
    qt_select_mv "select commission, sum(salary) from emps where commission = 100 group by commission order by commission;"
}
