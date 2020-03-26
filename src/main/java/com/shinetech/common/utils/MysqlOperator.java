package com.shinetech.common.utils;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;


/**
 * 预留，后续可能需要保存超时数据到mysql
 */
public class MysqlOperator {

    public static Logger logger = LoggerFactory.getLogger(MysqlOperator.class);


    /**
     * 获取mysql的连接
     * @param url
     * @param username
     * @param password
     * @return
     */
    public static Connection getConnection(String url, String username, String password) {

        if(StringUtils.isEmpty(url) || StringUtils.isEmpty(username) ||StringUtils.isEmpty(password)) {
            return null;
        }

        Connection conn = null;
        try {
            // 拆到配置文件中
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        return conn;
    }




    public static void main(String[] args) {


    }
}