import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;

import src.*;

public class Test {

    private transient RecordManager recman;
    private transient BufferedWriter writer;

    Test(String textFile) throws IOException {
        recman = RecordManagerFactory.createRecordManager("database");
        writer = new BufferedWriter(new FileWriter(textFile));
    }

    public static void main(String[] args) {
        try {
            Test tester = new Test("spider_result.txt");
            tester.test();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void test() throws IOException {
        // Loads all the database
        HTree urlPageIDMapForward =
                (HTree) HTree.load(recman, recman.getNamedObject("urlPageIDMapForward"));
        HTree urlPageIDMapBackward =
                (HTree) HTree.load(recman, recman.getNamedObject("urlPageIDMapBackward"));

        HTree wordIDMapBackward =
                (HTree) HTree.load(recman, recman.getNamedObject("wordIDMapBackward"));
        HTree parentIDPageInfoMap =
                (HTree) HTree.load(recman, recman.getNamedObject("parentIDPageInfoMap"));

        FastIterator iter = urlPageIDMapBackward.keys();
        Object pageID = iter.next();
        boolean started = false;

        while (pageID != null) {

            String url = (String) urlPageIDMapBackward.get(pageID);
            PageInfo pageInfo = (PageInfo) parentIDPageInfoMap.get(pageID);

            // TODO: add page title in pageInfo
            if (!started) {
                writer.write("PageTitlePlaceHolder");
                started = true;
            } else {
                writer.write("\n" + "PageTitlePlaceHolder");
            }

            writer.write("\n" + url);

            // TODO: add page size in pageInfo
            writer.write("\n" + pageInfo.getDate() + ", " + "SizePlaceHolder");


            String keywordsFreq = "";
            int count = 1;
            String wordFreqMapName = "wordFreqMap" + pageID;
            HTree wordFreqMap = HTree.load(recman, recman.getNamedObject(wordFreqMapName));
            FastIterator wordIter = wordFreqMap.keys();
            Object wordID = wordIter.next();

            while (wordID != null & count < 11) {
                int wordFreq = (int) wordFreqMap.get(wordID);
                keywordsFreq += (String) wordIDMapBackward.get(wordID) + " " + wordFreq + "; ";
                wordID = wordIter.next();
                count++;
            }
            writer.write("\n" + keywordsFreq);

            ArrayList<String> childUrls = pageInfo.getchildUrls();
            for (String childUrl : childUrls) {
                writer.write("\n" + childUrl);
            }

            writer.write(
                    "\n---------------------------------------------------------------------------------------------------");
            pageID = iter.next();


        }
        writer.close();
    }
}
