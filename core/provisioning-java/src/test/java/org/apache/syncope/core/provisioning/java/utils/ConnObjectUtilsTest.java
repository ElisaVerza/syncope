package org.apache.syncope.core.provisioning.java.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.*;

import org.apache.syncope.common.lib.Attr;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedByteArray;


@RunWith(value=Parameterized.class)
public class ConnObjectUtilsTest {
    enum ObjectType {
        GUARDEDSTRING,
        GUARDEDBYTEARRAY,
        OTHER,
        BYTE
    }

    private GuardedString gsPass;
    private GuardedByteArray gbaPass;
    private String strPass;
    private Object passToWrite;
    private Object objPass;
    private ConnObjectUtils cou;
    private ObjectType actualObj;
    private String fiql = "TestName";
    private String attrName = "TestAttrName";
    private boolean isSetNull;
    private Set<Attribute> set = new HashSet<Attribute>();
    private Attr attrRet;

    @Mock
    TemplateUtils templateUtils;
    @Mock
    RealmDAO realmDAO;
    @Mock
    UserDAO userDAO;
    @Mock
    ExternalResourceDAO resourceDAO;
    @Mock
    PasswordGenerator passwordGenerator;
    @Mock
    MappingManager mappingManager;
    @Mock
    AnyUtilsFactory anyUtilsFactory;


    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{     
            {ObjectType.GUARDEDSTRING, "Test", false},
            {ObjectType.OTHER, "Test", false},
            {ObjectType.OTHER, 1, true},
            {ObjectType.OTHER, null, false},
            {ObjectType.BYTE, "Test".getBytes(), false},
        });
    }

    public ConnObjectUtilsTest(ObjectType actualObj, Object passToWrite, boolean isSetNull) {
        this.actualObj = actualObj;
        this.passToWrite = passToWrite;
        this.isSetNull = isSetNull;
    }

    @Before
    public void getPassSetUp(){
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
            else if(actualObj == ObjectType.GUARDEDSTRING || actualObj == ObjectType.OTHER){
                assertEquals(passToWrite, attrRet.getValues().get(0));
            }
            else if(actualObj == ObjectType.BYTE){
                assertEquals(Base64.getEncoder().encodeToString((byte[]) passToWrite), attrRet.getValues().get(0));
            }
            assertEquals(attrName, attrRet.getSchema());
        }
        else{
            assertEquals(0, ConnObjectUtils.getConnObjectTO(fiql, set).getAttrs().size());
        }
        assertEquals(fiql, ConnObjectUtils.getConnObjectTO(fiql, set).getFiql());
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
