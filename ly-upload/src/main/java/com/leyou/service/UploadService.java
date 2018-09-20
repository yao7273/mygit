package com.leyou.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.exception.LyException;
import com.leyou.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@EnableConfigurationProperties(UploadProperties.class)
public class UploadService {

    //使用fastDFS上传文件，注册客户端，会自动经过tracker，想storage存储文件
    @Autowired
    private FastFileStorageClient storageClient;

    //通过配置类传入基本数据
    @Autowired
    private UploadProperties prop;

    //支持得文件类型
    //private static final List<String> suffixes = Arrays.asList("image/png","image/jpg");


    public String upload(MultipartFile file) {

        try {
            //1.图片信息得校验
            //1.1校验文件类型（除了规定得类型，其他不可上传）
            String contentType = file.getContentType();
            if(!prop.getAllowFileTypes().contains(contentType)){
                log.info("上传失败，文件类型不匹配",contentType);
                throw new LyException(HttpStatus.BAD_REQUEST,"文件类型不匹配");
            }

            //1.2校验图片类型
            //读取图片内容，如果返回值为null，表示不是图片
            BufferedImage image = ImageIO.read(file.getInputStream());
            if(image == null){
                log.info("上传失败，文件内容不合要求",contentType);
                throw new LyException(HttpStatus.BAD_REQUEST,"文件损坏");
            }

            //2.保存图片
            // 2.1、生成保存目录,保存到nginx所在目录的html下，这样可以直接通过nginx来访问到
//            File dir = new File("D:\\Soft\\leyou\\nginx-1.12.2\\html\\");
         /*   File dir = new File(prop.getLocalPath());
            //如果文件夹不存在，则新建
            if(!dir.exists()){
                dir.mkdirs();
            }

            //2.2保存图片
            file.transferTo(new File(dir,file.getOriginalFilename()));*/

            //使用storeClient,fastDFS上传
            //文件得后缀名
            String fileExtNname = StringUtils.substringAfterLast(file.getOriginalFilename(),".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), fileExtNname, null);

            //2.3拼接图片地址
            // String url = prop.getBaseUrl() + file.getOriginalFilename();
             String url = prop.getBaseUrl() + storePath.getFullPath();
            System.out.println(url);
            return url;

        } catch (Exception e) {
           throw new LyException(500,"文件上传异常");
        }
    }
}
