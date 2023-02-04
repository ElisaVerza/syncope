package org.apache.syncope.core.provisioning.java.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.*;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnyObjectDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAGroupDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAUserDAO;
import org.apache.syncope.core.persistence.jpa.entity.*;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAProvision;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAAnyTemplatePullTask;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedByteArray;
import org.mockito.Mockito;


@RunWith(value=Parameterized.class)
public class ConnObjectUtilsTest {
    public enum ObjType {
        GUARDEDSTRING,
        GUARDEDBYTEARRAY,
        OTHER,
        BYTE
    }

    private GuardedString gsPass;
    private GuardedByteArray gbaPass;
    private String strPass;
    private final Object passToWrite;
    private Object objPass;
    private ConnObjectUtils cou;
    private static ObjType actualObj;
    private final String fiql = "TestName";
    private final String attrName = "TestAttrName";
    private final boolean isSetNull;
    private Set<Attribute> set = new HashSet<Attribute>();
    private Attr attrRet;
    private AnyUtilsFactory anyUtilsFactory;
    private final UserDAO userDAO = mock(JPAUserDAO.class);
    private final TemplateUtils templateUtils = mock(TemplateUtils.class);
    @Mock
    RealmDAO realmDAO;
    @Mock
    ExternalResourceDAO resourceDAO;
    @Mock
    PasswordGenerator passwordGenerator;
    @Mock
    MappingManager mappingManager;



    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{     
            {ObjType.GUARDEDSTRING, "Test", false},
            /*{ObjType.OTHER, "Test", false},
            {ObjType.OTHER, 1, true},
            {ObjType.OTHER, null, false},
            {ObjType.BYTE, "Test".getBytes(), false},*/
        });
    }

    public ConnObjectUtilsTest(ObjType actualObj, Object passToWrite, boolean isSetNull) {
        this.actualObj = actualObj;
        this.passToWrite = passToWrite;
        this.isSetNull = isSetNull;
    }

    @Before
    public void getPassSetUp(){
        anyUtilsFactory = mock(JPAAnyUtilsFactory.class, RETURNS_DEEP_STUBS);
        cou = new ConnObjectUtils(templateUtils, realmDAO, userDAO, resourceDAO, passwordGenerator, mappingManager, anyUtilsFactory);
        switchCase();
        Attribute attr;
        if(!isSetNull){
            if(passToWrite!=null){
                attr = AttributeBuilder.build(attrName, objPass);
            }
            else{
                attr = AttributeBuilder.build(attrName);
            }
            set.add(attr);
            Attr[] attrArray = ConnObjectUtils.getConnObjectTO(fiql, set).getAttrs().toArray(new Attr[set.size()]);
            attrRet = attrArray[0];
        }
        else{
            set = null;
        }
    }


    @Test
    public void setPassTest(){
        if(passToWrite!=null){
            assertEquals(passToWrite.toString(), ConnObjectUtils.getPassword(objPass));
        }
    }

    @Test
    public void getConnObjectTOTest(){
        if(!isSetNull){
            if(passToWrite == null){
                System.out.println(attrRet.getSchema());
                assertEquals(0, attrRet.getValues().size());
            }
            else if(actualObj == ObjType.GUARDEDSTRING || actualObj == ObjType.OTHER){
                assertEquals(passToWrite, attrRet.getValues().get(0));
            }
            else if(actualObj == ObjType.BYTE){
                assertEquals(Base64.getEncoder().encodeToString((byte[]) passToWrite), attrRet.getValues().get(0));
            }
            assertEquals(attrName, attrRet.getSchema());
        }
        else{
            assertEquals(0, ConnObjectUtils.getConnObjectTO(fiql, set).getAttrs().size());
        }
        assertEquals(fiql, ConnObjectUtils.getConnObjectTO(fiql, set).getFiql());
    }

    @Test
    public void getAnyCRTest(){
        GroupDAO groupDAO = mock(JPAGroupDAO.class);
        AnyObjectDAO anyObjectDAO = mock(JPAAnyObjectDAO.class);
        Provision provision = mock(JPAProvision.class, RETURNS_DEEP_STUBS);
        PullTask pullTask = mock(PullTask.class, RETURNS_DEEP_STUBS);

        when(pullTask.getDestinationRealm().getFullPath()).thenReturn("my/testing/path");
        ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
        cob.setUid("UidTest");
        cob.setName("Conn obj name");
        ConnectorObject obj = cob.build();
        when(provision.getAnyType().getKind()).thenReturn(AnyTypeKind.USER);
        when(provision.getAnyType().getKey()).thenReturn("key test");
        when(anyUtilsFactory.getInstance(provision.getAnyType().getKind()).newAnyCR()).thenReturn(new UserCR());
        cou.getAnyCR(obj, pullTask, provision, true);
    }


    public void switchCase(){
        switch(actualObj) {
            case GUARDEDSTRING:
                char[] passwordToChar =String.valueOf(passToWrite).toCharArray();
                gsPass = new GuardedString(passwordToChar);
                objPass = gsPass;
              break;
            /*case GUARDEDBYTEARRAY:
                byte[] passwordToByte = passToWrite.getBytes();
                gbaPass = new GuardedByteArray(passwordToByte);
                break;*/
            case OTHER:
            case BYTE:
                objPass = passToWrite;
              break;
            default:

        }
    }
}
