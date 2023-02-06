package org.apache.syncope.core.provisioning.java.utils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.*;
import org.apache.syncope.core.persistence.api.entity.*;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.jpa.dao.JPARealmDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAUserDAO;
import org.apache.syncope.core.persistence.jpa.entity.*;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAProvision;
import org.apache.syncope.core.spring.policy.InvalidPasswordRuleConf;
import org.apache.syncope.core.spring.security.DefaultPasswordGenerator;
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
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedByteArray;
import org.apache.syncope.common.lib.request.AnyCR;


@RunWith(value=Parameterized.class)
public class ConnObjectUtilsTest {
    public enum ObjType {
        GUARDEDSTRING,
        GUARDEDBYTEARRAY,
        OTHER,
        BYTE
    }

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
    private ConnectorObject obj;
    private Provision provision;
    private PullTask pullTask;
    private final String anyCR;
    private AnyUtilsFactory anyUtilsFactory;
    private final UserDAO userDAO = mock(JPAUserDAO.class);
    private final TemplateUtils templateUtils = mock(TemplateUtils.class);
    private final boolean generatePass;
    private final boolean randomPass;
    private RealmDAO realmDAO;
    private final boolean realmIsNull;
    private final String passToSet = "PasswordToSet";

    @Mock
    ExternalResourceDAO resourceDAO;
    private PasswordGenerator passwordGenerator;
    @Mock
    MappingManager mappingManager;



    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
                {ObjType.GUARDEDSTRING, "Test", "uCR", false, true, true, false}, //realm not null
                {ObjType.OTHER, "Test", "uCR", false, true, true, true},//realm null
                {ObjType.OTHER, 1, "other", true, true, true, false},//anyCR = GroupCR
                {ObjType.OTHER, null, "uCR", false, false, true, false},//generatePass false
                //{ObjType.BYTE, "Test".getBytes(), false},
        });
    }

    public ConnObjectUtilsTest(ObjType actualObj, Object passToWrite, String anyCR ,boolean isSetNull, boolean generatePass, boolean randomPass, boolean realmIsNull) {
        this.actualObj = actualObj;
        this.passToWrite = passToWrite;
        this.anyCR = anyCR;
        this.isSetNull = isSetNull;
        this.generatePass = generatePass;
        this.randomPass = randomPass;
        this.realmIsNull = realmIsNull;
    }

    @Before
    public void getPassSetUp() throws InvalidPasswordRuleConf {
        mockGenerator();
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
        AnyCR aCR = cou.getAnyCR(obj, pullTask, provision, generatePass);
        if(anyCR.equals("uCR")){
            if(!generatePass || !randomPass){
                assertNull(((UserCR) aCR).getPassword());

            }
            else {
                assertEquals(passToSet, ((UserCR) aCR).getPassword());

                if(realmIsNull){
                    verify(realmDAO, times(0)).findAncestors(any(Realm.class));
                }
                else{
                    verify(realmDAO, times(1)).findAncestors(any(Realm.class));
                }
            }
        }
        else{
            assertTrue(aCR instanceof GroupCR);
        }
    }

    public void mockGenerator() throws InvalidPasswordRuleConf {
        AnyCR aCR;

        realmDAO = mock(JPARealmDAO.class, RETURNS_DEEP_STUBS);
        provision = mock(JPAProvision.class, RETURNS_DEEP_STUBS);
        pullTask = mock(PullTask.class, RETURNS_DEEP_STUBS);
        ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
        cob.setUid("UidTest");
        cob.setName("Conn obj name");
        obj = cob.build();
        passwordGenerator = mock(DefaultPasswordGenerator.class);
        anyUtilsFactory = mock(JPAAnyUtilsFactory.class, RETURNS_DEEP_STUBS);
        List<PasswordPolicy> passwordPolicies = new ArrayList<>();

        if(anyCR.equals("uCR")){
            aCR = new UserCR();
        }
        else{
            aCR = new GroupCR();
        }


        if(realmIsNull){
            when(realmDAO.findByFullPath(aCR.getRealm())).thenReturn(null);
        }
        else{
            when(realmDAO.findByFullPath(aCR.getRealm())).thenReturn(new JPARealm());
        }

        when(passwordGenerator.generate(passwordPolicies)).thenReturn(passToSet);
        when(pullTask.getDestinationRealm().getFullPath()).thenReturn("my/testing/path");
        when(provision.getAnyType().getKind()).thenReturn(AnyTypeKind.USER);
        when(provision.getAnyType().getKey()).thenReturn("key test");
        when(provision.getResource().isRandomPwdIfNotProvided()).thenReturn(randomPass);
        when(anyUtilsFactory.getInstance(provision.getAnyType().getKind()).newAnyCR()).thenReturn(aCR);
    }


    public void switchCase(){
        switch(actualObj) {
            case GUARDEDSTRING:
                char[] passwordToChar =String.valueOf(passToWrite).toCharArray();
                objPass = new GuardedString(passwordToChar);
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
