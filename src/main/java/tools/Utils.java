package tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.linguatools.disco.CorruptConfigFileException;
import de.linguatools.disco.DISCO;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;

/**
 * Created by aderenzis on 14/11/16.
 */
public class Utils {
    public static final Logger logger =  LogManager.getLogger("Utils");
    public static Dictionary dictionary = null;
    static Vector<String> vStopWords = null;
    static DISCO disco = null;

    public static boolean initializeDictionaries(String discoDbPath){

        try {
            dictionary = Dictionary.getDefaultResourceInstance();
            BufferedReader stoplistFile = new BufferedReader(new FileReader("./stoplist.txt"));
            String line;
            vStopWords = new Vector();
            while ((line = stoplistFile.readLine()) != null) {
                vStopWords.add(line);
            }
            stoplistFile.close();
            try {
                disco = new DISCO(discoDbPath, false);
            } catch (FileNotFoundException | CorruptConfigFileException ex) {
                logger.error("Error creating DISCO instance: ", ex);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Error Initializing Dictionaries: ", e);
            return false;
        }
    }

    public static boolean isWord (String wordQuery)
    {
        IndexWord idxWord = null;
        try {
            idxWord = dictionary.getIndexWord(POS.NOUN, wordQuery);
        } catch (JWNLException e) {
            logger.error("Problem with JWNL dictionaries: ", e );
        }
        if (idxWord == null)
            try {
                idxWord = dictionary.getIndexWord(POS.VERB, wordQuery);
            } catch (JWNLException e) {
                logger.error("Problem with JWNL dictionaries: ", e );
            }
        if (idxWord == null)
            try {
                idxWord = dictionary.getIndexWord(POS.ADJECTIVE, wordQuery);
            } catch (JWNLException e) {
                logger.error("Problem with JWNL dictionaries: ", e );
            }
        if (idxWord == null)
            try {
                idxWord = dictionary.getIndexWord(POS.ADVERB, wordQuery);
            } catch (JWNLException e) {
                logger.error("Problem with JWNL dictionaries: ", e );
            }
        return idxWord != null;
    }

    public static Vector separarTerminosAuxFine(String term)
    {
        Vector vec = new Vector();
        if(term.length()>0)
        {
            boolean mayus =false;
            String ret="";
            String retMayus="";
            char lastMayus=0;
            char charAux;
            if(term.charAt(0)>=65 && term.charAt(0)<=90) // Si es mayuscula la 1er letra如果第一个字母是大写的
            {
                charAux= (char) (term.charAt(0)+32); // guarda la minuscula小写保留
                ret=Character.toString(charAux); // ret almaceno la letra
                retMayus=Character.toString(charAux); // retMayus almaceno
                mayus=true;
            }
            else
                ret=Character.toString(term.charAt(0)); // si no es mayuscula se almacena el char en re如果没有大写，该字符将存储在ret中。
            for(int i=1;i< term.length();i++)
            {
                if(term.charAt(i)>=65 && term.charAt(i)<=90) // Si es una mayuscula如果是大写字母
                {

                    if(!mayus) //Es la primer Mayuscula它是第一个大写字母
                    {
                        if(retMayus.length()>1) // Ya existia anteriormente una seguidilla de mayusculas过去已经有一个大写字母的序列
                        {
                            if(isWord(lastMayus+ret))//es una palabra la ultima mayuscula + minusculas是一个单词的最后一个大写+小写字母
                            {
                                vec.add(retMayus.substring(0, retMayus.length()-1));
                                vec.add(lastMayus+ret);
                                lastMayus=0;
                            }
                            else
                            {
                                vec.add(retMayus);
                                vec.add(ret);
                                lastMayus=0;
                            }
                        }
                        else // No existia anteriormente una seguidilla de mayusculas以前没有大写字母序列
                            if(ret.length()>0)
                                vec.add(ret);

                        mayus=true;
                        charAux= (char) (term.charAt(i)+32);
                        ret=Character.toString(charAux);
                        retMayus=Character.toString(charAux);
                    }
                    else //No es la primer mayuscula consecutiva不是第一个连续的大写字母
                    {
                        charAux= (char) (term.charAt(i)+32);
                        retMayus = retMayus+charAux;
                        ret="";
                    }


                }
                else //No es una Mayuscula不是一个大写字母
                {
                    if(term.charAt(i) == 45 || term.charAt(i)== 95 || esNumero(term.charAt(i))) //  Si es _ o -
                    {
                        if(ret.length()>0) // si el guion esta despues de una acumulacion de Minusculas如果连字符在小写字母的堆积之后
                        {
                            vec.add(ret);
                            ret="";
                            retMayus="";
                        }
                        else if(retMayus.length()>0) // si el guion esta despues de una acumulacion de Mayusculas如果连字符在大写字母的堆积之后
                        {
                            vec.add(retMayus);
                            retMayus="";
                        }

                        mayus=false;
                    } // No es mayuscula ni _ ni - ni Numero// es una letra minuscula无论是_还是-还是Number//都不是小写字母。
                    else
                    {
                        if(mayus) // la Letra anterior era una mayuscula前一个字母是大写字母
                        {
                            lastMayus= (char) (term.charAt(i-1)+32);
                            ret=ret+term.charAt(i);
                            mayus=false;
                        }
                        else // la letra anterior no era mayuscula前一个字母没有大写
                        {
                            ret=ret+term.charAt(i);
                        }

                    }
                }
            }
            if(ret.length()>0 | retMayus.length()>1)
            {
                if(retMayus.length()>1) // Ya existia anteriormente una seguidilla de mayusculas
                {
                    if(lastMayus != 0 && ret.length()>0 && isWord(lastMayus+ret)) // Es un && porque si lastMayus es 0 no debe entrar al metodo isWord.
                    {
                        vec.add(retMayus.substring(0, retMayus.length()-1));
                        vec.add(lastMayus+ret);
                    }
                    else
                    {
                        if(retMayus.length()>1);
                        vec.add(retMayus);
                        if(ret.length()>0)
                            vec.add(ret);
                    }
                }
                else
                    vec.add(ret);
            }
        }
        try {
            vec = removeStopWords(vec);
        } catch (IOException e) {
            logger.error(e);
        }
        return vec;
    }

    public static Vector<String> removeStopWords(Vector v1) throws IOException
    {

        boolean b = v1.removeAll(vStopWords);
        return v1;
    }

    private static boolean esNumero(char charAt) {

        return (48<=charAt && charAt<=57);
    }

}