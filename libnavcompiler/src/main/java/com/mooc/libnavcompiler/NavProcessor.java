package com.mooc.libnavcompiler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.auto.service.AutoService;
import com.mooc.libnavannotation.ActivityDestination;
import com.mooc.libnavannotation.FragmentDestination;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * APPҳ�浼����Ϣ�ռ�ע�⴦����
 * <p>
 * AutoServiceע�⣺����ôһ��ǣ�annotationProcessor  project()Ӧ��һ��,����ʱ�����Զ�ִ�и����ˡ�
 * <p>
 * SupportedSourceVersionע��:����������֧�ֵ�jdk�汾
 * <p>
 * SupportedAnnotationTypes:������ע�⴦������Ҫ������Щע��
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({"com.mooc.libnavannotation.FragmentDestination", "com.mooc.libnavannotation.ActivityDestination"})
public class NavProcessor extends AbstractProcessor {
    private Messager messager;
    private Filer filer;
    private static final String OUTPUT_FILE_NAME = "destination.json";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        //��־��ӡ,��java�����²���ʹ��android.util.log.e()
        messager = processingEnv.getMessager();
        //�ļ�������
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //ͨ������������������roundEnv�ֱ��ȡ ��Ŀ�б�ǵ�FragmentDestination.class ��ActivityDestination.classע�⡣
        //��Ŀ�ľ���Ϊ���ռ���Ŀ����Щ�� ��ע������
        Set<? extends Element> fragmentElements = roundEnv.getElementsAnnotatedWith(FragmentDestination.class);
        Set<? extends Element> activityElements = roundEnv.getElementsAnnotatedWith(ActivityDestination.class);

        if (!fragmentElements.isEmpty() || !activityElements.isEmpty()) {
            HashMap<String, JSONObject> destMap = new HashMap<>();
            //�ֱ� ����FragmentDestination  �� ActivityDestination ע������
            //���ռ���destMap ���map�С��Դ˾��ܼ�¼�����е�ҳ����Ϣ��
            handleDestination(fragmentElements, FragmentDestination.class, destMap);
            handleDestination(activityElements, ActivityDestination.class, destMap);

            //app/src/main/assets
            FileOutputStream fos = null;
            OutputStreamWriter writer = null;
            try {
                //filer.createResource()��˼�Ǵ���Դ�ļ�
                //���ǿ���ָ��Ϊclass�ļ�����ĵط���
                //StandardLocation.CLASS_OUTPUT��java�ļ�����class�ļ���λ�ã�/app/build/intermediates/javac/debug/classes/Ŀ¼��
                //StandardLocation.SOURCE_OUTPUT��java�ļ���λ�ã�һ����/ppjoke/app/build/generated/source/apt/Ŀ¼��
                //StandardLocation.CLASS_PATH �� StandardLocation.SOURCE_PATH�õĲ��ָ࣬���������������Ҫָ�������ļ���pkg������
                FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", OUTPUT_FILE_NAME);
                String resourcePath = resource.toUri().getPath();
                messager.printMessage(Diagnostic.Kind.NOTE, "resourcePath:" + resourcePath);

                //����������Ҫ��json�ļ�������app/src/main/assets/Ŀ¼��,����������Զ��ַ�����һ����ȡ��
                //�Դ˱���׼ȷ��ȡ��Ŀ��ÿ�������ϵ� /app/src/main/assets/��·��
                String appPath = resourcePath.substring(0, resourcePath.indexOf("app") + 4);
                String assetsPath = appPath + "src/main/assets/";

                File file = new File(assetsPath);
                if (!file.exists()) {
                    file.mkdirs();
                }

                //�˴������Ƚ���д����
                File outPutFile = new File(file, OUTPUT_FILE_NAME);
                if (outPutFile.exists()) {
                    outPutFile.delete();
                }
                outPutFile.createNewFile();

                //����fastjson���ռ��������е�ҳ����Ϣ ת����JSON��ʽ�ġ���������ļ���
                String content = JSON.toJSONString(destMap);
                fos = new FileOutputStream(outPutFile);
                writer = new OutputStreamWriter(fos, "UTF-8");
                writer.write(content);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        return true;
    }

    private void handleDestination(Set<? extends Element> elements, Class<? extends Annotation> annotationClaz, HashMap<String, JSONObject> destMap) {
        for (Element element : elements) {
            //TypeElement��Element��һ�֡�
            //������ǵ�ע�������������ϡ����Կ���ֱ��ǿתһ�¡�ʹ�����õ�ȫ����
            TypeElement typeElement = (TypeElement) element;
            //ȫ����com.mooc.ppjoke.home
            String clazName = typeElement.getQualifiedName().toString();
            //ҳ���id.�˴������ظ�,ʹ��ҳ���������hascode����
            int id = Math.abs(clazName.hashCode());
            //ҳ���pageUrl�൱����ʿ��ת��ͼ�е�host://schem/path��ʽ
            String pageUrl = null;
            //�Ƿ���Ҫ��¼
            boolean needLogin = false;
            //�Ƿ���Ϊ��ҳ�ĵ�һ��չʾ��ҳ��
            boolean asStarter = false;
            //��Ǹ�ҳ����fragment ����activity���͵�
            boolean isFragment = false;

            Annotation annotation = element.getAnnotation(annotationClaz);
            if (annotation instanceof FragmentDestination) {
                FragmentDestination dest = (FragmentDestination) annotation;
                pageUrl = dest.pageUrl();
                asStarter = dest.asStarter();
                needLogin = dest.needLogin();
                isFragment = true;
            } else if (annotation instanceof ActivityDestination) {
                ActivityDestination dest = (ActivityDestination) annotation;
                pageUrl = dest.pageUrl();
                asStarter = dest.asStarter();
                needLogin = dest.needLogin();
                isFragment = false;
            }

            if (destMap.containsKey(pageUrl)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "��ͬ��ҳ�治����ʹ����ͬ��pageUrl��" + clazName);
            } else {
                JSONObject object = new JSONObject();
                object.put("id", id);
                object.put("needLogin", needLogin);
                object.put("asStarter", asStarter);
                object.put("pageUrl", pageUrl);
                object.put("className", clazName);
                object.put("isFragment", isFragment);
                destMap.put(pageUrl, object);
            }
        }
    }
}
