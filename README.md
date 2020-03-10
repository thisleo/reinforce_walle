# 360加固 + 美团Walle多渠道自动化打包

使用美团[walle](https://github.com/Meituan-Dianping/walle)进行多渠道打包可以节省大量的时间, 但是使用中发现一个问题:

*之前是使用walle打多渠道包, 然后将apk上传360进行加固; 后来发现加固之后的APP没有了渠道信息, 并且发现360加固完成后的自动重签名只有V1签名*

因此需要我们自己手动重签名, 需要调整实现流程: **加固——重签名——多渠道打包**



#### 我们希望实现gradle脚本实现自动化加固、签名和多渠道打包

*目前可以使用[瓦力多渠道打包的Python脚本](https://github.com/yangchong211/YCWalleHelper)实现自动化打包, 但是使用的`python2.7`版本*



另一种思路是使用gradle脚本实现:

**在此感谢原创作者：天子卿**

**附作者文章链接：https://juejin.im/post/5c825ac6f265da2db9129bd0**

通过作者文章完成此demo, 如有侵权,请联系删除

![](https://gitee.com/cdyiwhy/imgbed/raw/master/img/20200310093427.png)

> > > ### 首先是集成Walle
> > >
> > > 这里只简单写下Gradle插件使用方式, 如有其他需求请查看主页[Meituan-Dianping](https://github.com/Meituan-Dianping)/**[walle](https://github.com/Meituan-Dianping/walle)**
> > >
> > > #### 配置build.gradle
> > >
> > > 在位于项目的根目录 `build.gradle` 文件中添加Walle Gradle插件的依赖， 如下：
> > >
> > > ```groovy
> > > buildscript {
> > >     dependencies {
> > >         classpath 'com.meituan.android.walle:plugin:1.1.6'
> > >     }
> > > }
> > > ```
> > >
> > > 并在当前App的 `build.gradle` 文件中apply这个插件，并添加上用于读取渠道号的AAR
> > >
> > > ```groovy
> > > apply plugin: 'walle'
> > > 
> > > dependencies {
> > >     compile 'com.meituan.android.walle:library:1.1.6'
> > > }
> > > ```
> > >
> > > #### 配置插件
> > >
> > > ```groovy
> > > walle {
> > >     // 指定渠道包的输出路径
> > >     apkOutputFolder = new File("${project.buildDir}/outputs/channels");
> > >     // 定制渠道包的APK的文件名称
> > >     apkFileNameFormat = '${appName}-${packageName}-${channel}-${buildType}-v${versionName}-${versionCode}-${buildTime}.apk';
> > >     // 渠道配置文件
> > >     channelFile = new File("${project.getProjectDir()}/channel")
> > > }
> > > ```
> > >
> > > #### 如何获取渠道信息
> > >
> > > 在需要渠道等信息时可以通过下面代码进行获取
> > >
> > > ```java
> > > String channel = WalleChannelReader.getChannel(this.getApplicationContext());
> > > ```
> > >
> > > 

***

### 实现原理

##### 1. 加固

​	加固过程： 浏览了360加固官网，整个加固过程其实很简单，主要有以下的三个步骤：

 1. 输入360加固平台的帐号、密码

 2. 将签名文件上传到加固平台

 3. 上传需要加固的apk文件进行加固

    关键加固命令行代码如下：

    ```groovy
    /**
     * 360加固
     * @param apk 加固的原始apk File
     * @param outputPath 输出目录
     */
    def reinforceApk(File apk,outputPath) {
        println "--- 360 reinforceApk start! ---"
        println "reinforce apk:" + apk
        if(apk == null || !apk.exists()) {
            throw new FileNotFoundException('apk is not exists and cannot reinforce')
            println "---360 reinforceApk throw exception and forced stop!---"
        }
        exec {
            commandLine "{命令执行符号}", "-c", "java -jar ${REINFORCE_JAR} -login  ${REINFORCE_NAME} ${REINFORCE_PASSWORD}"
            commandLine "{命令执行符号}", "-c", "java -jar ${REINFORCE_JAR} -showsign"
            commandLine "{命令执行符号}", "-c", "java -jar ${REINFORCE_JAR} -jiagu ${apk} ${outputPath}"
        }
        println "--- 360 reinforce end! ---"
    }
    ```

    **说明:** 系统环境不同，`{命令执行符号}`也会不同（Linux系统：sh ；Mac系统：bash ；windows系统：powershell）;



##### 2. 重签名

​	加固完成后,对加固apk进行重签名;

​	使用AndroidSDK中的build-tools目录下, 使用压缩对齐工具和签名工具完成重签名, 步骤如下:

 1. 对齐，对Apk文件进行存档对齐优化，确保所有的未压缩数据都从文件的开始位置以指定的对齐方式排列

 2. 签名，选择Signature V2   

    ```groovy
    commandLine "{命令执行符号}","-c", "{zipalign工具的文件路径} -v -p 4  {已加固的apk文件路径} {对齐后输出的apk文件路径}"
    commandLine "{命令执行符号}", "-c", "{apksigner工具的文件路径} sign --ks {签名文件的位置} --ks-key-alias {alias别名} --ks-pass pass:{签名文件存储的密码} --key-pass pass:{alias密码} --out {签名后输出的apk文件} {对齐后输出的apk文件路径}"
    ```

    

##### 3. 多渠道打包

​	签名完成后, 使用walle进行多渠道打包

​	平时使用walle多渠道打包，只需要在app/build.gradle下配置插件，指定渠道包的输出路径和渠道配置文件即	可，最后在Android studio的Terminal中输入./gradlew assembleReleaseChannels，任务执行完成后在指定的	输出路径下生成多个对应的渠道包。具体的流程和细节可参考[官方介绍](https://github.com/Meituan-Dianping/walle)。

​	这种多渠道打包方式是全自动化构建，很难去干涉到构建流程，不符合我们的需求   

1. 在app/build.gradle配置插件时，在官方介绍中并没有找到指定源APK输入路径的方式，估计打包插件默认使用的是app/build/outputs/apk/release下的apk文件，这样就没办法对不同文件路径下的已加固apk包进行多渠道打包。

2. 打包任务设置在assembleRelease之后执行，这个执行依赖封装在插件内部，外部很难修改打包任务依赖于加固任务，在加固任务之后执行。     

     

   除了上面的多渠道打包方式之后，walle还提供了另外一种多渠道打包方式，用命令行执行walle提供的walle-cli-all.jar执行打包操作，只需要一条打包命令即可完成打包。   

   ```groovy
   commandLine "sh", "-c", "java -jar {walle-cli-all.jar文件路径} batch -f {渠道文件路径} {要加渠道的apk文件路径} {渠道包的输出路径}"
   ```

   walle-cli-all.jar文件下载地址：[官方:walle-cli-all.jar](https://github.com/Meituan-Dianping/walle/tree/master/walle-cli), [其他开发提供的编译版本](https://github.com/vclub/vclub.github.io/raw/master/walle-cli-all.jar)

   这里为什么会有两个版本呢! 因为发现了一个比较坑的地方官方的版本打完包会发现在系统9.0（P）下无法正常安装, 相关问题可以查看[Issue](https://github.com/Meituan-Dianping/walle/issues/264), 当然你也可以自己拉取源码编译

   

---

### 整体流程

首先，将加固和打包操作封装成自动化操作，利用gradle脚本构建加固任务。

为了代码解耦，我们不在app/build.gradle里面实现加固任务，而是重新建一个gradle文件來实现具体的加固和多渠道打包过程，在app/build.gradle只需要通过`apply from: '×××.gradle'`引用这个gradle文件即可，

![](https://gitee.com/cdyiwhy/imgbed/raw/master/img/20200310092933.png)



当需要修改加固的一些代码逻辑时，只需要在这个gradle文件里面修改。

引入工具包。根据自己的系统环境，在[加固助手网页](http://jiagu.360.cn/#/global/download)选择对应的加固助手工具，下载后将里面的jiagu文件夹拷贝到自己项目的根目录下;

在[walle-cli-jar下载链接](https://github.com/Meituan-Dianping/walle/tree/master/walle-cli)下载jar包到自己项目中。

![](https://gitee.com/cdyiwhy/imgbed/raw/master/img/20200310093055.png)

确定加固任务的时机。加固任务时机应该在release包生成之后，那么加固任务应该依赖于assembleRelease这个任务，并且设置在这个任务之后执行。

接下来就是我们的基本流程了

 	1. 找到release包, 一般在`app/build/outputs/apk/release/`路径下
 	2. 执行加固命令，将release包路径设置到命令中，并指定加固apk文件的输出路径
 	3. 找到已加固的apk文件，对已加固apk文件进行对齐、重签名。（360已加固的apk文件会在原有的release文件名后面加上"_jiagu"）
 	4. 找到重新签名的apk文件，执行多渠道打包命令。（重签名后的文件名是在原有文件名后面加上"_sign"）

```groovy
task assembleReinforceRelease() {
    group '360reinforce'
    dependsOn("assembleRelease")

    doLast {
        cleanFilesPath(CHANNEL_APKS_PATH)   //清空上一次生成的渠道包
        def releaseApkFile = findApkFile(DEFAULT_APK_PATH, "-release")  //遍历文件，寻找release包
        println "--release--1-" + releaseApkFile
        if (releaseApkFile != null) {
            reinforceApk(releaseApkFile, SOURCE_APK_PATH)   //执行加固
            def reinforceApk = findApkFile(SOURCE_APK_PATH, "_jiagu")  //寻找已加固的apk包
            println "--jiagu--2-" + reinforceApk
            if(reinforceApk != null) {
                zipAlignApk(reinforceApk) // zip对齐
                def zipAlignApk = findApkFile(SOURCE_APK_PATH, "_zip")
                if (zipAlignApk != null) {
                    signApkV2(zipAlignApk)  //使用V2重签名
                    def signatureApk = findApkFile(SOURCE_APK_PATH, "_sign")
                    println "--sign--3-" + signatureApk
                    if(signatureApk != null) {
                        buildChannelApks(signatureApk)  //执行多渠道打包
                        renameChannelApkFiles(CHANNEL_APKS_PATH) //重命名渠道包
                    }
                }
            }
        }
    }
}
```

代码优化:

1. 流程中涉及360加固平台帐号密码等敏感信息，可以将这部分信息放到签名信息所在的文件（eg：keystore.properties）中统一管理，然后将这些信息加载到gradle文件中；
2. 各种输入输出的文件路径定义为常量，便于修改和管理；

加固方法:

```groovy
/**
 * 360加固
 * @param apk 加固的原始apk File
 * @param outputPath 输出目录
 */
def reinforceApk(File apk, outputPath) {
    println "--- 360 reinforceApk start! ---"
    println "reinforce apk:" + apk
    if (apk == null || !apk.exists()) {
        println "---360 reinforceApk throw exception and forced stop!---"
        throw new FileNotFoundException('apk is not exists and cannot reinforce')
    }
    def file = new File(outputPath)
    if (!file.exists()) {
        file.mkdir()
    }
    exec {
        commandLine getCommand(), "-c", "java -jar ${REINFORCE_JAR} -login  ${REINFORCE_NAME} ${REINFORCE_PASSWORD}"
        commandLine getCommand(), "-c", "java -jar ${REINFORCE_JAR} -showsign"
        commandLine getCommand(), "-c", "java -jar ${REINFORCE_JAR} -jiagu ${apk} ${outputPath}"
    }
    println "--- 360 reinforce end! ---"
}
```

压缩对齐方法:

```groovy
/**
 * 加固后的apk 对齐压缩
 * @param apk 已加固apk
 * @return 返回对齐压缩后的apk
 */
def zipAlignApk(File apk) {
    if (apk == null || !apk.exists()) {
        println "---zipalign reinforceApk throw exception and forced stop!---"
        throw new FileNotFoundException('apk is not exists and cannot reinforce')
    }
    def BUILD_TOOL_PATH = getAndroidSdkPath()
    def APK_NAME = getApkName() + "_jiagu_zip.apk"
    def file = new File("${SOURCE_APK_PATH}/${APK_NAME}")
    if (file.exists()) {
        file.delete()
    }

    exec {
        commandLine getCommand(), "-c", "${BUILD_TOOL_PATH}zipalign -v -p 4 ${apk} ${SOURCE_APK_PATH}/${APK_NAME}"
    }
}
```

签名方法:

```groovy
/**
 * 对apk签名
 * @param zipApk 压缩对齐后的apk
 * @return 签名后的apk
 */
def signApkV2(File zipApk) {
    if (zipApk == null || !zipApk.exists()) {
        println "---sign zipApk throw exception and forced stop!---"
        throw new FileNotFoundException('apk is not exists and cannot reinforce')
    }
    def BUILD_TOOL_PATH = getAndroidSdkPath()
    def APK_NAME = "app-release_" + getApkVersionName() + "_jiagu_zip_sign.apk"
    def file = new File("${SOURCE_APK_PATH}/${APK_NAME}")
    if (file.exists()) {
        file.delete()
    }
    exec {
        commandLine getCommand(), "-c", "${BUILD_TOOL_PATH}apksigner sign --ks ${KEY_PATH} --ks-key-alias ${ALIAS} --ks-pass pass:${KEY_PASSWORD} --key-pass pass:${ALIAS_PASSWORD} --out ${SOURCE_APK_PATH}/${APK_NAME} ${zipApk}"
    }
}
```

添加渠道信息:

```groovy
/**
 * 对签名后的apk添加渠道信息
 * @param apk 已签名apk
 * @return 添加渠道信息后的apk
 */
def buildChannelApks(File apk) {
    if (apk == null || !apk.exists()) {
        println "---Channel build Apk throw exception and forced stop!---"
        throw new FileNotFoundException('apk is not exists and cannot reinforce')
    }
    def file = new File(CHANNEL_APKS_PATH)
    if (!file.exists()) {
        file.mkdir()
    }
    def APK_NAME = getApkName() + "_jiagu_zip_sign.apk"

    exec {
        //java -jar walle-cli-all.jar batch -f /Users/Meituan/walle/app/channel  /Users/Meituan/walle/app/build/outputs/apk/app.apk
        commandLine getCommand(), "-c", "java -jar ${WALLE_JAR} batch -f ${WALLE_CHANNELS_CONFIG} ${SOURCE_APK_PATH}/${APK_NAME} ${CHANNEL_APKS_PATH}"
    }
}
```

重命名apk:

```groovy
/**
 * 重命名apk
 * @param path 渠道apk目录路径
 * @return
 */
def renameChannelApkFiles(path) {
//    def APK_NAME = "app-release_" + getApkVersionName() + "_jiagu_zip_sign.apk"
    def regex = getApkName() + "_jiagu_zip_sign"
    def dir = new File(path+"/")
    dir.listFiles().each {file ->
        if (file.name =~ /${regex}.*\.apk/) {
            String newName = file.name
            newName = newName.replaceAll(~/_jiagu/, "")
            newName = newName.replaceAll(~/_zip/, "")
            newName = newName.replaceAll(~/_sign/, "")
            file.renameTo(new File(file.getParent(), newName))
        }
    }
}
```

**涉及到的各种常量，各种密钥名、路径都要根据自己的实际情况修改：**

```groovy
/*加载keystore.properties信息到该gradle文件中*/
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
keystoreProperties.load(new FileInputStream(keystorePropertiesFile))

ext {
    /*加固*/
    REINFORCE_JAR = "${project.rootDir}/jiagu/jiagu.jar"
    REINFORCE_NAME = keystoreProperties['360_NAME'] //360加固账号
    REINFORCE_PASSWORD = keystoreProperties['360_PASSWORD'] //360加固密码
    KEY_PATH = keystoreProperties['storeFile'] //密钥路径
    KEY_PASSWORD = keystoreProperties['storePassword'] //密钥密码
    ALIAS = keystoreProperties['keyAlias'] //密钥别名
    ALIAS_PASSWORD = keystoreProperties['keyPassword'] //别名密码
    SOURCE_APK_PATH = "${project.buildDir}/bakApk"  //源apk文件路径
    DEFAULT_APK_PATH = "${project.buildDir}/outputs/apk/release" //默认release文件路径
    /*多渠道打包*/
    WALLE_JAR = "${project.rootDir}/walle-cli-all.jar"
    WALLE_CHANNELS_CONFIG = "${project.rootDir}/app/channel"  //渠道配置文件
    CHANNEL_APKS_PATH = "${project.buildDir}/outputs/channels"  //渠道Apk输出路径
}
```

![](https://gitee.com/cdyiwhy/imgbed/raw/master/img/20200310093134.png)

**再次感谢原创作者：天子卿**

**附作者文章链接：https://juejin.im/post/5c825ac6f265da2db9129bd0**

**在作者的启发下完成此demo, 如果对你有帮助请不要吝啬star**