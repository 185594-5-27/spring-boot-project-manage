package com.github.spring.boot.project.manage.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.util.*;


/**
 * @author linzf
 * @since 11:05
 * 类描述：svn 操作工具类
 */
public class SvnUtil {

    private static Logger logger = LoggerFactory.getLogger(SvnUtil.class);

    public static void main(String[] args) {
        //svnHistory("svn://172.20.9.110/web/web_taxctrl/web_taxctrl_open/trunk/src/taxctrl_open_app_manage_producer", "linzefeng", "linzefeng");
        checkOut("svn://172.20.9.110/web/web_taxctrl/web_taxctrl_open/trunk/src/taxctrl_open_eureka", "linzefeng", "linzefeng", "D:\\test", 5800);
    }


    /**
     * 功能描述：获取svn的版本历史信息
     * @param svnUrl       svn地址
     * @param userName     svn账号
     * @param password     svn密码
     * @return 返回版本日志的数据集合
     */
    public static List<SVNLogEntry> svnHistory(String svnUrl, String userName, String password) {
        //定义svn版本库的URL。
        SVNURL repositoryURL;
        //定义版本库。
        SVNRepository repository;
        try {
            //获取SVN的URL。
            repositoryURL = SVNURL.parseURIEncoded(svnUrl);
            //根据URL实例化SVN版本库。
            repository = SVNRepositoryFactory.create(repositoryURL);
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(userName, password);
            repository.setAuthenticationManager(authManager);
            return listEntries(repository);
        } catch (SVNException e) {
            logger.error("获取版本库异常，异常原因：{}", e.getMessage());
            return null;
        }
    }



    /**
     * 功能描述：获取最新版本的svn文件信息
     *
     * @param svnUrl       svn地址
     * @param userName     svn账号
     * @param password     svn密码
     * @param checkOutPath svn文件检出到本地的地址
     */
    public static void checkOut(String svnUrl, String userName, String password, String checkOutPath) {
        checkOut(svnUrl, userName, password, checkOutPath, 0);
    }

    /**
     * 功能描述：获取指定版本的文件信息
     *
     * @param svnUrl       svn地址
     * @param userName     svn账号
     * @param password     svn密码
     * @param checkOutPath svn文件检出到本地的地址
     */
    public static void checkOut(String svnUrl, String userName, String password, String checkOutPath, long version) {
        SVNURL repositoryURL;
        try {
            repositoryURL = SVNURL.parseURIEncoded(svnUrl);
        } catch (SVNException e) {
            logger.error("获取svn地址,失败原因：{}", e.getMessage());
            return;
        }
        // 获取svn服务器连接
        SVNClientManager svnClientManager = authSvn(svnUrl, userName, password);
        if (svnClientManager == null) {
            return;
        }
        // 获取到的文件的保存路径
        File wcDir = new File(checkOutPath);
        if (!wcDir.exists()) {
            wcDir.mkdirs();
        }
        SVNUpdateClient updateClient = svnClientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        try {
            /*
             SVNDepth.EMPTY：只获取到当前的目录，不获取当前目录底下的文件
             SVNDepth.FILES：获取该文件夹底下的所有文件
             SVNDepth.IMMEDIATES：只获取当前目录，不递归获取下级目录的文件
             SVNDepth.INFINITY：获取该文件夹底下的所有的文件，不包含底下的文件。
             */
            //  将文件信息检出到本地的地址
            if (version == 0) {
                updateClient.doCheckout(repositoryURL, wcDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.FILES, true);
            } else {
                updateClient.doCheckout(repositoryURL, wcDir, SVNRevision.create(version), SVNRevision.create(version), SVNDepth.INFINITY, true);
            }
        } catch (SVNException e) {
            logger.error("文件检出到本地失败，请确认账号和密码以及svn地址是否正确：{}", e.getMessage());
        }

    }

    /**
     * 功能描述：获取SVN的连接
     *
     * @param svnUrl   svn地址
     * @param userName 账号
     * @param password 密码
     * @return 返回SVN的连接对象
     */
    public static SVNClientManager authSvn(String svnUrl, String userName, String password) {
        // 初始化版本库
        setupLibrary();
        // 创建库连接
        SVNRepository repository;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(svnUrl));
        } catch (SVNException e) {
            logger.error("创建svn连接失败，失败原因：{}", e.getMessage());
            return null;
        }
        // 身份验证
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(userName, password.toCharArray());
        // 创建身份验证管理器
        repository.setAuthenticationManager(authManager);
        DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
        return SVNClientManager.newInstance(options, authManager);
    }

    /**
     * 功能描述：通过不同的协议初始化版本库
     */
    private static void setupLibrary() {
        // 初始化通过使用 http:// 和 https:// 访问
        DAVRepositoryFactory.setup();
        // 初始化通过使用svn:// 和 svn+xxx://访问
        SVNRepositoryFactoryImpl.setup();
        // 初始化通过使用file:///访问
        FSRepositoryFactory.setup();
    }

    /**
     * 功能描述：递归获取版本日志的信息
     *
     * @param repository 获取的SVNRepository对象
     * @return 返回版本日志的数据集合
     * @throws SVNException 抛出异常
     */
    private static List<SVNLogEntry> listEntries(SVNRepository repository) throws SVNException {
        Collection logEntries = repository.log(new String[]{""}, null, 0, -1, true, true);
        List<SVNLogEntry> svnLogEntryList = new ArrayList<>(logEntries.size());
        for (Iterator entries = logEntries.iterator(); entries.hasNext(); ) {
            SVNLogEntry logEntry = (SVNLogEntry) entries.next();
            svnLogEntryList.add(logEntry);
            logger.info("---------------------------------------------");
            logger.info("revision: " + logEntry.getRevision());
            logger.info("author: " + logEntry.getAuthor());
            logger.info("date: " + logEntry.getDate());
            logger.info("log message: " + logEntry.getMessage());
            // 获取改变的文件的内容
            if (logEntry.getChangedPaths().size() > 0) {
                logger.info("\n");
                logger.info("changed paths:");
                Set changedPathsSet = logEntry.getChangedPaths().keySet();
                for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext(); ) {
                    SVNLogEntryPath entryPath = logEntry.getChangedPaths().get(changedPaths.next());
                    logger.info(" "
                            + entryPath.getType()
                            + " "
                            + entryPath.getPath()
                            + ((entryPath.getCopyPath() != null) ? " (from "
                            + entryPath.getCopyPath() + " revision "
                            + entryPath.getCopyRevision() + ")" : ""));
                }
            }
        }
        return svnLogEntryList;
    }

}
