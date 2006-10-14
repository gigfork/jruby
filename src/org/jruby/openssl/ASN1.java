/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.openssl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
 
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Enumeration;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERBoolean;
import org.bouncycastle.asn1.DERBitString;

import org.jruby.IRuby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubySymbol;

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class ASN1 {
    private static Map SYM_TO_OID = new IdentityHashMap();
    private static Map OID_TO_SYM = new IdentityHashMap();

    synchronized static Map getOIDLookup(IRuby runtime) {
        Object val = SYM_TO_OID.get(runtime);
        if(null == val) {
            val = new HashMap(org.bouncycastle.asn1.x509.X509Name.DefaultLookUp);
            ((Map)val).put("streetaddress",org.bouncycastle.asn1.x509.X509Name.DefaultLookUp.get("street"));
            ((Map)val).put("organizationname",org.bouncycastle.asn1.x509.X509Name.DefaultLookUp.get("o"));
            ((Map)val).put("commonname",org.bouncycastle.asn1.x509.X509Name.DefaultLookUp.get("cn"));
            ((Map)val).put("countryname",org.bouncycastle.asn1.x509.X509Name.DefaultLookUp.get("c"));
            ((Map)val).put("basicconstraints",new DERObjectIdentifier("2.5.29.19"));
            ((Map)val).put("keyusage",new DERObjectIdentifier("2.5.29.15"));
            ((Map)val).put("subjectkeyidentifier",new DERObjectIdentifier("2.5.29.14"));
            ((Map)val).put("authoritykeyidentifier",new DERObjectIdentifier("2.5.29.35"));
            ((Map)val).put("extendedkeyusage",new DERObjectIdentifier("2.5.29.37"));
            ((Map)val).put("subjectaltname",new DERObjectIdentifier("2.5.29.17"));
            ((Map)val).put("rsa-sha1",new DERObjectIdentifier("1.2.840.113549.1.1.5"));
            SYM_TO_OID.put(runtime,val);
        }
        return (Map)val;
    }

    synchronized static Map getSymLookup(IRuby runtime) {
        Object val = OID_TO_SYM.get(runtime);
        if(null == val) {
            val = new HashMap(org.bouncycastle.asn1.x509.X509Name.DefaultSymbols);
            ((Map)val).put(org.bouncycastle.asn1.x509.X509Name.DefaultLookUp.get("street"),"streetAddress");
            ((Map)val).put(new DERObjectIdentifier("2.5.29.19"),"basicConstraints");
            ((Map)val).put(new DERObjectIdentifier("2.5.29.15"),"keyUsage");
            ((Map)val).put(new DERObjectIdentifier("2.5.29.14"),"subjectKeyIdentifier");
            ((Map)val).put(new DERObjectIdentifier("2.5.29.35"),"authorityKeyIdentifier");
            ((Map)val).put(new DERObjectIdentifier("2.5.29.37"),"extendedKeyUsage");
            ((Map)val).put(new DERObjectIdentifier("2.5.29.17"),"subjectAltName");
            ((Map)val).put(new DERObjectIdentifier("1.2.840.113549.1.1.5"),"RSA-SHA1");
            OID_TO_SYM.put(runtime,val);
        }
        return (Map)val;
    }

    private final static Object[][] ASN1_INFO = {
        {"EOC", null, null },
        {"BOOLEAN", org.bouncycastle.asn1.DERBoolean.class, "Boolean" },
        {"INTEGER", org.bouncycastle.asn1.DERInteger.class, "Integer" }, 
        {"BIT_STRING",  org.bouncycastle.asn1.DERBitString.class, "BitString" },
        {"OCTET_STRING",  org.bouncycastle.asn1.DEROctetString.class, "OctetString" },
        {"NULL",  org.bouncycastle.asn1.DERNull.class, "Null" },
        {"OBJECT",  org.bouncycastle.asn1.DERObjectIdentifier.class, "ObjectId" },
        {"OBJECT_DESCRIPTOR",  null, null },
        {"EXTERNAL",  null, null },
        {"REAL",  null, null },
        {"ENUMERATED",  org.bouncycastle.asn1.DEREnumerated.class, "Enumerated" },
        {"EMBEDDED_PDV",  null, null },
        {"UTF8STRING",  org.bouncycastle.asn1.DERUTF8String.class, "UTF8String" },
        {"RELATIVE_OID",  null, null },
        {"[UNIVERSAL 14]",  null, null },
        {"[UNIVERSAL 15]",  null, null },
        {"SEQUENCE",  org.bouncycastle.asn1.DERSequence.class, "Sequence" },
        {"SET",  org.bouncycastle.asn1.DERSet.class, "Set" },
        {"NUMERICSTRING",  org.bouncycastle.asn1.DERNumericString.class, "NumericString" },
        {"PRINTABLESTRING",  org.bouncycastle.asn1.DERPrintableString.class, "PrintableString" },
        {"T61STRING",  org.bouncycastle.asn1.DERT61String.class, "T61String" },
        {"VIDEOTEXSTRING", null, null },
        {"IA5STRING",  org.bouncycastle.asn1.DERIA5String.class, "IA5String" },
        {"UTCTIME",  org.bouncycastle.asn1.DERUTCTime.class, "UTCTime" },
        {"GENERALIZEDTIME",  org.bouncycastle.asn1.DERGeneralizedTime.class, "GeneralizedTime" },
        {"GRAPHICSTRING",  null, null },
        {"ISO64STRING",  null, null },
        {"GENERALSTRING",  org.bouncycastle.asn1.DERGeneralString.class, "GeneralString" },
        {"UNIVERSALSTRING",  org.bouncycastle.asn1.DERUniversalString.class, "UniversalString" },
        {"CHARACTER_STRING",  null, null },
        {"BMPSTRING", org.bouncycastle.asn1.DERBMPString.class, "BMPString" }};

    private final static Map CLASS_TO_ID = new HashMap();
    private final static Map RUBYNAME_TO_ID = new HashMap();
    
    static {
        for(int i=0;i<ASN1_INFO.length;i++) {
            if(ASN1_INFO[i][1] != null) {
                CLASS_TO_ID.put(ASN1_INFO[i][1],new Integer(i));
            }
            if(ASN1_INFO[i][2] != null) {
                RUBYNAME_TO_ID.put(ASN1_INFO[i][2],new Integer(i));
            }
        }
    }

    public static int idForClass(Class type) {
        Integer v = (Integer)CLASS_TO_ID.get(type);
        return null == v ? -1 : v.intValue();
    }

    public static int idForRubyName(String name) {
        Integer v = (Integer)RUBYNAME_TO_ID.get(name);
        return null == v ? -1 : v.intValue();
    }

    public static Class classForId(int id) {
        return (Class)(ASN1_INFO[id][1]);
    }
    
    public static void createASN1(IRuby runtime, RubyModule ossl) {
        RubyModule mASN1 = ossl.defineModuleUnder("ASN1");
        mASN1.defineClassUnder("ASN1Error",ossl.getClass("OpenSSLError"));

        CallbackFactory asncb = runtime.callbackFactory(ASN1.class);
        mASN1.defineSingletonMethod("traverse",asncb.getSingletonMethod("traverse",IRubyObject.class));
        mASN1.defineSingletonMethod("decode",asncb.getSingletonMethod("decode",IRubyObject.class));
        mASN1.defineSingletonMethod("decode_all",asncb.getSingletonMethod("decode_all",IRubyObject.class));

        List ary = new ArrayList();
        mASN1.setConstant("UNIVERSAL_TAG_NAME",runtime.newArray(ary));
        for(int i=0;i<ASN1_INFO.length;i++) {
            if(((String)(ASN1_INFO[i][0])).charAt(0) != '[') {
                ary.add(runtime.newString(((String)(ASN1_INFO[i][0]))));
                mASN1.setConstant(((String)(ASN1_INFO[i][0])),runtime.newFixnum(i));
            } else {
                ary.add(runtime.getNil());
            }
        }

        RubyClass cASN1Data = mASN1.defineClassUnder("ASN1Data",runtime.getObject());
        cASN1Data.attr_accessor(new IRubyObject[]{runtime.newString("value"),runtime.newString("tag"),runtime.newString("tag_class")});
        CallbackFactory asn1datacb = runtime.callbackFactory(ASN1Data.class);
        cASN1Data.defineSingletonMethod("new",asn1datacb.getOptSingletonMethod("newInstance"));
        cASN1Data.defineMethod("initialize",asn1datacb.getOptMethod("initialize"));
        cASN1Data.defineMethod("to_der",asn1datacb.getMethod("to_der"));

        RubyClass cASN1Primitive = mASN1.defineClassUnder("Primitive",cASN1Data);
        cASN1Primitive.attr_accessor(new IRubyObject[]{runtime.newString("tagging")});
        CallbackFactory primcb = runtime.callbackFactory(ASN1Primitive.class);
        cASN1Primitive.defineSingletonMethod("new",primcb.getOptSingletonMethod("newInstance"));
        cASN1Primitive.defineMethod("initialize",primcb.getOptMethod("initialize"));
        cASN1Primitive.defineMethod("to_der",primcb.getMethod("to_der"));

        RubyClass cASN1Constructive = mASN1.defineClassUnder("Constructive",cASN1Data);
        cASN1Constructive.includeModule(runtime.getModule("Enumerable"));
        cASN1Constructive.attr_accessor(new IRubyObject[]{runtime.newString("tagging")});
        CallbackFactory concb = runtime.callbackFactory(ASN1Constructive.class);
        cASN1Constructive.defineSingletonMethod("new",concb.getOptSingletonMethod("newInstance"));
        cASN1Constructive.defineMethod("initialize",concb.getOptMethod("initialize"));
        cASN1Constructive.defineMethod("to_der",concb.getMethod("to_der"));
        cASN1Constructive.defineMethod("each",concb.getMethod("each"));

        mASN1.defineSingletonMethod("Boolean",asncb.getOptSingletonMethod("fact_Boolean"));
        mASN1.defineSingletonMethod("Integer",asncb.getOptSingletonMethod("fact_Integer"));
        mASN1.defineSingletonMethod("Enumerated",asncb.getOptSingletonMethod("fact_Enumerated"));
        mASN1.defineSingletonMethod("BitString",asncb.getOptSingletonMethod("fact_BitString"));
        mASN1.defineSingletonMethod("OctetString",asncb.getOptSingletonMethod("fact_OctetString"));
        mASN1.defineSingletonMethod("UTF8String",asncb.getOptSingletonMethod("fact_UTF8String"));
        mASN1.defineSingletonMethod("NumericString",asncb.getOptSingletonMethod("fact_NumericString"));
        mASN1.defineSingletonMethod("PrintableString",asncb.getOptSingletonMethod("fact_PrintableString"));
        mASN1.defineSingletonMethod("T61String",asncb.getOptSingletonMethod("fact_T61String"));
        mASN1.defineSingletonMethod("VideotexString",asncb.getOptSingletonMethod("fact_VideotexString"));
        mASN1.defineSingletonMethod("IA5String",asncb.getOptSingletonMethod("fact_IA5String"));
        mASN1.defineSingletonMethod("GraphicString",asncb.getOptSingletonMethod("fact_GraphicString"));
        mASN1.defineSingletonMethod("ISO64String",asncb.getOptSingletonMethod("fact_ISO64String"));
        mASN1.defineSingletonMethod("GeneralString",asncb.getOptSingletonMethod("fact_GeneralString"));
        mASN1.defineSingletonMethod("UniversalString",asncb.getOptSingletonMethod("fact_UniversalString"));
        mASN1.defineSingletonMethod("BMPString",asncb.getOptSingletonMethod("fact_BMPString"));
        mASN1.defineSingletonMethod("Null",asncb.getOptSingletonMethod("fact_Null"));
        mASN1.defineSingletonMethod("ObjectId",asncb.getOptSingletonMethod("fact_ObjectId"));
        mASN1.defineSingletonMethod("UTCTime",asncb.getOptSingletonMethod("fact_UTCTime"));
        mASN1.defineSingletonMethod("GeneralizedTime",asncb.getOptSingletonMethod("fact_GeneralizedTime"));
        mASN1.defineSingletonMethod("Sequence",asncb.getOptSingletonMethod("fact_Sequence"));
        mASN1.defineSingletonMethod("Set",asncb.getOptSingletonMethod("fact_Set"));

        mASN1.defineClassUnder("Boolean",cASN1Primitive);
        mASN1.defineClassUnder("Integer",cASN1Primitive);
        mASN1.defineClassUnder("Enumerated",cASN1Primitive);
        RubyClass cASN1BitString = mASN1.defineClassUnder("BitString",cASN1Primitive);
        mASN1.defineClassUnder("OctetString",cASN1Primitive);
        mASN1.defineClassUnder("UTF8String",cASN1Primitive);
        mASN1.defineClassUnder("NumericString",cASN1Primitive);
        mASN1.defineClassUnder("PrintableString",cASN1Primitive);
        mASN1.defineClassUnder("T61String",cASN1Primitive);
        mASN1.defineClassUnder("VideotexString",cASN1Primitive);
        mASN1.defineClassUnder("IA5String",cASN1Primitive);
        mASN1.defineClassUnder("GraphicString",cASN1Primitive);
        mASN1.defineClassUnder("ISO64String",cASN1Primitive);
        mASN1.defineClassUnder("GeneralString",cASN1Primitive);
        mASN1.defineClassUnder("UniversalString",cASN1Primitive);
        mASN1.defineClassUnder("BMPString",cASN1Primitive);
        mASN1.defineClassUnder("Null",cASN1Primitive);
        RubyClass cASN1ObjectId = mASN1.defineClassUnder("ObjectId",cASN1Primitive);
        mASN1.defineClassUnder("UTCTime",cASN1Primitive);
        mASN1.defineClassUnder("GeneralizedTime",cASN1Primitive);
        mASN1.defineClassUnder("Sequence",cASN1Constructive);
        mASN1.defineClassUnder("Set",cASN1Constructive);

        cASN1ObjectId.defineSingletonMethod("register",asncb.getOptSingletonMethod("objectid_register"));
    }

    public static IRubyObject objectid_register(IRubyObject recv, IRubyObject[] args) {
        DERObjectIdentifier deroi = new DERObjectIdentifier(args[0].toString());
        getOIDLookup(recv.getRuntime()).put(args[1].toString().toLowerCase(),deroi);
        getOIDLookup(recv.getRuntime()).put(args[2].toString().toLowerCase(),deroi);
        getSymLookup(recv.getRuntime()).put(deroi,args[1].toString());
        return recv.getRuntime().getTrue();
    }
    
    public static IRubyObject fact_Boolean(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("Boolean").callMethod("new",args);
    }

    public static IRubyObject fact_Integer(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("Integer").callMethod("new",args);
    }

    public static IRubyObject fact_Enumerated(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("Enumerated").callMethod("new",args);
    }

    public static IRubyObject fact_BitString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("BitString").callMethod("new",args);
    }

    public static IRubyObject fact_OctetString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("OctetString").callMethod("new",args);
    }

    public static IRubyObject fact_UTF8String(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("UTF8String").callMethod("new",args);
    }

    public static IRubyObject fact_NumericString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("NumericString").callMethod("new",args);
    }

    public static IRubyObject fact_PrintableString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("PrintableString").callMethod("new",args);
    }

    public static IRubyObject fact_T61String(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("T61String").callMethod("new",args);
    }

    public static IRubyObject fact_VideotexString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("VideotexString").callMethod("new",args);
    }

    public static IRubyObject fact_IA5String(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("IA5String").callMethod("new",args);
    }

    public static IRubyObject fact_GraphicString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("GraphicString").callMethod("new",args);
    }

    public static IRubyObject fact_ISO64String(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("ISO64String").callMethod("new",args);
    }

    public static IRubyObject fact_GeneralString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("GeneralString").callMethod("new",args);
    }

    public static IRubyObject fact_UniversalString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("UniversalString").callMethod("new",args);
    }

    public static IRubyObject fact_BMPString(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("BMPString").callMethod("new",args);
    }

    public static IRubyObject fact_Null(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("Null").callMethod("new",args);
    }

    public static IRubyObject fact_ObjectId(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("ObjectId").callMethod("new",args);
    }

    public static IRubyObject fact_UTCTime(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("UTCTime").callMethod("new",args);
    }

    public static IRubyObject fact_GeneralizedTime(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("GeneralizedTime").callMethod("new",args);
    }

    public static IRubyObject fact_Sequence(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("Sequence").callMethod("new",args);
    }

    public static IRubyObject fact_Set(IRubyObject recv, IRubyObject[] args) {
        return ((RubyModule)recv).getClass("Set").callMethod("new",args);
    }

    public static IRubyObject traverse(IRubyObject recv, IRubyObject a) {
        System.err.println("WARNING: unimplemented method called: traverse");
        return null;
    }

    private final static DateFormat dateF = new SimpleDateFormat("yyMMddHHmmssz");
    private static IRubyObject decodeObj(RubyModule asnM,Object v) throws Exception {
        int ix = idForClass(v.getClass());
        String v_name = ix == -1 ? null : (String)(ASN1_INFO[ix][2]);
        if(null != v_name) {
            RubyClass c = asnM.getClass(v_name);
            if(v instanceof DERBitString) {
                String va = new String(((DERBitString)v).getBytes(),"ISO8859_1");
                return c.callMethod("new",asnM.getRuntime().newString(va));
            } else if(v instanceof DERString) {
                String val = ((DERString)v).getString();
                if(v instanceof DERUTF8String) {
                    val = new String(val.getBytes("UTF-8"),"ISO8859-1");
                }
                return c.callMethod("new",asnM.getRuntime().newString(val));
            } else if(v instanceof DERSequence) {
                List l = new ArrayList();
                for(Enumeration enm = ((DERSequence)v).getObjects(); enm.hasMoreElements(); ) {
                    l.add(decodeObj(asnM,enm.nextElement()));
                }
                return c.callMethod("new",asnM.getRuntime().newArray(l));
            } else if(v instanceof DERSet) {
                List l = new ArrayList();
                for(Enumeration enm = ((DERSet)v).getObjects(); enm.hasMoreElements(); ) {
                    l.add(decodeObj(asnM,enm.nextElement()));
                }
                return c.callMethod("new",asnM.getRuntime().newArray(l));
            } else if(v instanceof DERNull) {
                return c.callMethod("new",asnM.getRuntime().getNil());
            } else if(v instanceof DERInteger) {
                return c.callMethod("new",RubyNumeric.str2inum(asnM.getRuntime(),asnM.getRuntime().newString(((DERInteger)v).getValue().toString()),10));
            } else if(v instanceof DERUTCTime) {
                Date d = dateF.parse(((DERUTCTime)v).getTime());
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                IRubyObject[] argv = new IRubyObject[6];
                argv[0] = asnM.getRuntime().newFixnum(cal.get(Calendar.YEAR));
                argv[1] = asnM.getRuntime().newFixnum(cal.get(Calendar.MONTH));
                argv[2] = asnM.getRuntime().newFixnum(cal.get(Calendar.DAY_OF_MONTH));
                argv[3] = asnM.getRuntime().newFixnum(cal.get(Calendar.HOUR));
                argv[4] = asnM.getRuntime().newFixnum(cal.get(Calendar.MINUTE));
                argv[5] = asnM.getRuntime().newFixnum(cal.get(Calendar.SECOND));
                return c.callMethod("new",asnM.getRuntime().getClass("Time").callMethod("utc",argv));
            } else if(v instanceof DERObjectIdentifier) {
                String av = ((DERObjectIdentifier)v).getId();
                return c.callMethod("new",asnM.getRuntime().newString(av));
            } else if(v instanceof DEROctetString) {
                String va = new String(((DEROctetString)v).getOctets(),"ISO8859_1");
                return c.callMethod("new",asnM.getRuntime().newString(va));
            } else if(v instanceof DERBoolean) {
                return c.callMethod("new",((DERBoolean)v).isTrue() ? asnM.getRuntime().getTrue() : asnM.getRuntime().getFalse());
            } else {
                System.out.println("Should handle: " + v.getClass().getName());
            }
        } else if(v instanceof DERTaggedObject) {
            RubyClass c = asnM.getClass("ASN1Data");
            IRubyObject val = decodeObj(asnM, ((DERTaggedObject)v).getObject());
            IRubyObject tag = asnM.getRuntime().newFixnum(((DERTaggedObject)v).getTagNo());
            IRubyObject tag_class = asnM.getRuntime().newSymbol("CONTEXT_SPECIFIC");
            return c.callMethod("new",new IRubyObject[]{val,tag,tag_class});
        }

        System.err.println("v: " + v + "[" + v.getClass().getName() + "]");
        return null;
    }

    public static IRubyObject decode(IRubyObject recv, IRubyObject obj) throws Exception {
        obj = OpenSSLImpl.to_der_if_possible(obj);
        RubyModule asnM = (RubyModule)recv;
        ASN1InputStream asis = new ASN1InputStream(obj.toString().getBytes("PLAIN"));
        IRubyObject ret = decodeObj(asnM, asis.readObject());
        return ret;
    }

    public static IRubyObject decode_all(IRubyObject recv, IRubyObject a) {
        System.err.println("WARNING: unimplemented method called: decode_all");
        return null;
    }

    public static class ASN1Data extends RubyObject {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            ASN1Data result = new ASN1Data(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }

        public ASN1Data(IRuby runtime, RubyClass type) {
            super(runtime,type);
        }

        protected void asn1Error() {
            asn1Error(null);
        }

        protected void asn1Error(String msg) {
            throw new RaiseException(getRuntime(), (RubyClass)(((RubyModule)(getRuntime().getModule("OpenSSL").getConstant("ASN1"))).getConstant("ASN1Error")), msg, true);
        }

        public IRubyObject initialize(IRubyObject[] args) {
            checkArgumentCount(args,3,3);
            IRubyObject value = args[0];
            IRubyObject tag = args[1];
            IRubyObject tag_class = args[2];
            if(!(tag_class instanceof RubySymbol)) {
                asn1Error("invalid tag class");
            }
            if(tag_class.toString().equals(":UNIVERSAL") && RubyNumeric.fix2int(tag) > 31) {
                asn1Error("tag number for Universal too large");
            }

            this.callMethod("tag=", tag);
            this.callMethod("value=", value);
            this.callMethod("tag_class=", tag_class);

            return this;
        }

        public IRubyObject to_der() {
            System.err.println("WARNING: unimplemented method called: asn1data#to_der");
            return this;
        }

        protected IRubyObject defaultTag() {
            int i = idForRubyName(getMetaClass().getRealClass().getBaseName());
            if(i != -1) {
                return getRuntime().newFixnum(i);
            } else {
                return getRuntime().getNil();
            }
        }
    }

    public static class ASN1Primitive extends ASN1Data {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            ASN1Data result = new ASN1Primitive(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }

        public ASN1Primitive(IRuby runtime, RubyClass type) {
            super(runtime,type);
        }

        public IRubyObject initialize(IRubyObject[] args) {
            checkArgumentCount(args,1,4);
            IRubyObject value = args[0];
            IRubyObject tag = getRuntime().getNil();
            IRubyObject tagging = getRuntime().getNil();
            IRubyObject tag_class = getRuntime().getNil();
            if(args.length>1) {
                tag = args[1];
                if(args.length>2) {
                    tagging = args[2];
                    if(args.length>3) {
                        tag_class = args[3];
                    }
                }
                if(tag.isNil()) {
                    asn1Error("must specify tag number");
                }
                if(tagging.isNil()) {
                    tagging = getRuntime().newSymbol("EXPLICIT");
                }
                if(!(tagging instanceof RubySymbol)) {
                    asn1Error("invalid tag default");
                }
                if(tag_class.isNil()) {
                    tag_class = getRuntime().newSymbol("CONTEXT_SPECIFIC");
                }
                if(!(tag_class instanceof RubySymbol)) {
                    asn1Error("invalid tag class");
                }
                if(tagging.toString().equals(":IMPLICIT") && RubyNumeric.fix2int(tag) > 31) {
                    asn1Error("tag number for Universal too large");
                }
            } else {
                tag = defaultTag();
                tagging = getRuntime().getNil();
                tag_class = getRuntime().newSymbol("UNIVERSAL");
            }
            if("ObjectId".equals(getMetaClass().getRealClass().getBaseName())) {
                String v = (String)(getSymLookup(getRuntime()).get(new DERObjectIdentifier(value.toString())));
                if(v != null) {
                    value = getRuntime().newString(v);
                }
            }

            this.callMethod("tag=",tag);
            this.callMethod("value=",value);
            this.callMethod("tagging=",tagging);
            this.callMethod("tag_class=",tag_class);

            return this;
        }

        public IRubyObject to_der() {
            System.err.println("WARNING: unimplemented method called: asn1prim#to_der");
            return this;
        }
    }

    public static class ASN1Constructive extends ASN1Data {
        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
            ASN1Data result = new ASN1Constructive(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }

        public ASN1Constructive(IRuby runtime, RubyClass type) {
            super(runtime,type);
        }

        public IRubyObject initialize(IRubyObject[] args) {
            checkArgumentCount(args,1,4);
            IRubyObject value = args[0];
            IRubyObject tag = getRuntime().getNil();
            IRubyObject tagging = getRuntime().getNil();
            IRubyObject tag_class = getRuntime().getNil();
            if(args.length>1) {
                tag = args[1];
                if(args.length>2) {
                    tagging = args[2];
                    if(args.length>3) {
                        tag_class = args[3];
                    }
                }
                if(tag.isNil()) {
                    asn1Error("must specify tag number");
                }
                if(tagging.isNil()) {
                    tagging = getRuntime().newSymbol("EXPLICIT");
                }
                if(!(tagging instanceof RubySymbol)) {
                    asn1Error("invalid tag default");
                }
                if(tag_class.isNil()) {
                    tag_class = getRuntime().newSymbol("CONTEXT_SPECIFIC");
                }
                if(!(tag_class instanceof RubySymbol)) {
                    asn1Error("invalid tag class");
                }
                if(tagging.toString().equals(":IMPLICIT") && RubyNumeric.fix2int(tag) > 31) {
                    asn1Error("tag number for Universal too large");
                }
            } else {
                tag = defaultTag();
                tagging = getRuntime().getNil();
                tag_class = getRuntime().newSymbol("UNIVERSAL");
            }
            this.callMethod("tag=",tag);
            this.callMethod("value=",value);
            this.callMethod("tagging=",tagging);
            this.callMethod("tag_class=",tag_class);

            return this;
        }

        public IRubyObject to_der() {
            System.err.println("WARNING: unimplemented method called: asn1cons#to_der");
            return this;
        }

        public IRubyObject each() {
            System.err.println("WARNING: unimplemented method called: asn1cons#each");
            return getRuntime().getNil();
        }
    }
}// ASN1
