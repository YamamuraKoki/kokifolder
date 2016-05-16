package jp.co.iccom.yamamura_koki.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CalcurateSystem {
	private static ArrayList<String> rcdFile;

	public static void main(String[] args) throws IOException {
		HashMap<String, String> branches = new HashMap<String, String>();
		HashMap<String, String> commodities = new HashMap<String, String>();
		HashMap<String, Long> branchValueResult = new HashMap<String, Long>();
		HashMap<String, Long> commodityValueResult = new HashMap<String, Long>();

		if(args.length != 1){
			System.out.println("予期せぬエラーが発生しました");
			return;
		}

		//支店定義ファイルの読み込み//
		if(!fileRead(args[0], "branch.lst", branches, branchValueResult, "支店", "^\\d{3}$")){
			return;
		}

		//商品定義ファイルの読み込み//
		if(!fileRead(args[0], "commodity.lst", commodities, commodityValueResult, "商品", "^\\w{8}$")){
			return;
		}

		//売上ファイル読み込み//
		//ディレクトリ内のファイルを出力//
		File dir = new File(args[0]);
		//フィルタリングをかけてファイル名を列挙
		String[] filterFile = dir.list(new MyFilter());
		//ファイルの数をlengthでカウント//
		for(int i=0; i < filterFile.length; i++){
			//rcdファイル名を数字と拡張子に分割//
			String [] rcdFilterFile = filterFile[i].split("\\.", -1);
			//rcdファイル名の数値を文字列→数値に変更//
			int rcdFileChangeNumber = Integer.parseInt(rcdFilterFile[0]);
			//連番でない場合エラーを表示する//
			int rcdFileDifferent = rcdFileChangeNumber-i;
			if(rcdFileDifferent != 1){
				System.out.println("売上ファイル名が連番になっていません");
				return;
			}

			//売上ファイルの呼び出し//
			try{
				if(!rcdFileRead(args[0], filterFile[i], rcdFilterFile[0], branches, commodities, 0, 1)){
					return;
				}

				//支店別合計の計算//
				if(!fileCalcurate(branchValueResult, rcdFile, 0, 2)){
					return;
				}

				//商品別売上の計算//
				if(!fileCalcurate(commodityValueResult, rcdFile, 1, 2)){
					return;
				}
			}
			catch(NumberFormatException e){
				System.out.println("予期せぬエラーが発生しました");
				return;
			}
		}

		//売上集計ファイルの作成//
		if(!fileWrite(args[0], "branch.out", branchValueResult, branches)){
			return;
		}

		if(!fileWrite(args[0], "commodity.out", commodityValueResult, commodities)){
			return;
		}
	}

	//売上集計書き込みのメソッド//
	private static boolean fileWrite(String path, String fileName, HashMap<String, Long> Value,
			HashMap<String, String> code) {
		//新規作成//
		File MakeFile = new File(path + File.separator + fileName);

		//書き込み//
		File Write = MakeFile;
		FileWriter fw = null;
		try {
			fw = new FileWriter(Write);
		}
		catch (IOException e) {
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw = new PrintWriter(bw);
		//並び替え//
		try{
			List<Map.Entry<String,Long>> calcurateSort = new ArrayList<Map.Entry<String,Long>>(Value.entrySet());
			Collections.sort(calcurateSort, new Comparator<Map.Entry<String,Long>>() {
				public int compare(Entry<String,Long> entry1, Entry<String,Long> entry2) {
					return ((Long)entry2.getValue()).compareTo((Long)entry1.getValue());
				}
			});
			for (Entry<String,Long> calcurateSortResult : calcurateSort) {
				pw.println(calcurateSortResult.getKey() + "," + code.get(calcurateSortResult.getKey()) + ","+ calcurateSortResult.getValue());
			}
		}
		finally{
			pw.close();
		}

		return true;
	}

	//定義ファイル読み込みのメソッド//
	private static boolean fileRead(String path, String fileName, HashMap<String, String> mapname,
			HashMap<String, Long> valueName, String fName, String match){
		try{
			BufferedReader read = new BufferedReader(new FileReader(path + File.separator + fileName));
			try{
				String str;
				while((str = read.readLine()) != null){
					String[]split = str.split(",", -1);
					//要素数2個以外はエラー//
					if(split.length != 2){
						System.out.println(fName + "定義ファイルのフォーマットが不正です");
						return false;
					}
					if(!split[0].matches(match)){
						System.out.println(fName + "定義ファイルのフォーマットが不正です");
						return false;
					}
					mapname.put(split[0] , split[1] );
					valueName.put(split[0] , 0L);
				}
			}
			finally{
				read.close();
			}
			return true;
		}

		catch(IOException e){
			System.out.println(fName + "定義ファイルが存在しません");
			return false;
		}

	}

	//売上金額合計のメソッド//
	private static boolean fileCalcurate(HashMap<String, Long> valueName, ArrayList<String> fileName,
			int codeName, int salesA){
		//元々入ってる金額//
		Long amount = valueName.get(fileName.get(codeName));
		//売上//
		Long sales = Long.valueOf(fileName.get(salesA));
		//元金額に売上を足す//
		Long amountSales = amount + sales;
		//上書きしてMapに格納//
		valueName.put(fileName.get(codeName), amountSales);

		//合計金額の桁数が10桁以上の場合エラーを表示する//
		if(valueName.get(fileName.get(codeName)) >= 10000000000L){
			System.out.println("合計金額が10桁を超えました");
			return false;
		}
		return true;
	}
	//売上ファイル読み込み//
	private static boolean rcdFileRead(String path, String filterFile, String splitName, HashMap<String, String> bName,
			HashMap<String, String> cName,  int branchCode , int commodityCode){
		BufferedReader rcdRead = null;
		try{
			rcdRead = new BufferedReader(new FileReader(path + File.separator + filterFile));
			String str;
			rcdFile = new ArrayList<String>();

			//売上ファイルが8桁以外であればエラーを表示する//
			if(!splitName.matches("^\\d{8}$")) {
				System.out.println(filterFile + "のファイル名が8桁ではありません");
				return false;
			}

			while((str = rcdRead.readLine()) != null){
				rcdFile.add(str);
			}
			//売上ファイルの中身が2行以下、4行以上のエラー表示//
			if(rcdFile.size() != 3){
				System.out.println(filterFile + "のフォーマットが不正です");
				return false;
			}
			//売上ファイルの中身の支店コードがHashmapに格納されているものと異なる場合エラー表示
			if(!bName.containsKey(rcdFile.get(branchCode))){
				System.out.println(filterFile + "の支店コードが不正です");
				return false;
			}
			//売上ファイルの中身の商品コードがHashmapに格納されているものと異なる場合エラー表示//
			if(!cName.containsKey(rcdFile.get(commodityCode))){
				System.out.println(filterFile + "の商品コードが不正です");
				return false;
			}
		}
		catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}
		finally{
			try {
				rcdRead.close();
			} catch (IOException e) {
				System.out.println("予期せぬエラーが発生しました");
				return false;
			}
			catch(IndexOutOfBoundsException e){
				System.out.println("予期せぬエラーが発生しました");
				return false;
			}
			catch(NullPointerException e) {
				System.out.println("予期せぬエラーが発生しました");
				return false;
			}
		}
		return true;
	}
}
//売上ファイルのフィルターのメソッド//
class MyFilter implements FilenameFilter{

	public boolean accept(File dir, String name){

		//拡張子の"."を探す//
		int index = name.lastIndexOf(".");

		//"."以下の文字列を取り出して全て小文字にする//
		String ext = name.substring(index+1).toLowerCase();

		//ファイルの宣言//
		File salesFile = new File(dir, name);

		//拡張子が"rcd"と一致し対象がファイルであれば取り出す//
		if(salesFile.isFile() &&  ext.equals("rcd") == true) {return true;}

		//それ以外のファイルはリストアップしない//
		return false;
	}
}