# -*- coding: utf-8 -*-
"""
postal_code_mapping.json を自動生成するスクリプト

処理概要:
1. 日本郵便から全国の郵便番号データ (utf_all.zip) をダウンロード・解凍
2. 気象庁の地域コードと都道府県のマッピングを定義
3. 郵便番号データから、上位3桁の郵便番号と都道府県の対応表を作成
4. 3の対応表と2のマッピングを使い、上位3桁の郵便番号 -> 地域コード のマッピングを生成
5. 結果を postal_code_mapping.json として出力
"""
import csv
import io
import json
import os
import requests
import zipfile

# --- 定数定義 ---

# 日本郵便 郵便番号データ (UTF-8版, 全国一括)
POSTAL_CODE_URL = "https://www.post.japanpost.jp/zipcode/dl/utf/zip/utf_ken_all.zip"
ZIP_FILE_NAME = "utf_ken_all.zip"
CSV_FILE_NAME = "utf_ken_all.csv"

# 出力先
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets')
OUTPUT_FILE_NAME = "postal_code_mapping.json"

# 都道府県名 -> 気象庁 地域コードのマッピング
# livedoor Weather API 互換API(tsukumijima.net)の仕様に基づく
# 参考: https://kaden1000.com/livedoor-weather-hacks-api/
PREFECTURE_TO_AREA_CODE = {
    '北海道': '016010',  # 札幌を代表とする
    '青森県': '020010',
    '岩手県': '030010',
    '宮城県': '040010',
    '秋田県': '050010',
    '山形県': '060010',
    '福島県': '070010',
    '茨城県': '080010',
    '栃木県': '090010',
    '群馬県': '100010',
    '埼玉県': '110010',
    '千葉県': '120010',
    '東京都': '130010',
    '神奈川県': '140010',
    '新潟県': '150010',
    '富山県': '160010',
    '石川県': '170010',
    '福井県': '180010',
    '山梨県': '190010',
    '長野県': '200010',
    '岐阜県': '210010',
    '静岡県': '220010',
    '愛知県': '230010',
    '三重県': '240010',
    '滋賀県': '250010',
    '京都府': '260010',
    '大阪府': '270000',
    '兵庫県': '280010',
    '奈良県': '290010',
    '和歌山県': '300010',
    '鳥取県': '310010',
    '島根県': '320010',
    '岡山県': '330010',
    '広島県': '340010',
    '山口県': '350010',
    '徳島県': '360010',
    '香川県': '370000',
    '愛媛県': '380010',
    '高知県': '390010',
    '福岡県': '400010',
    '佐賀県': '410010',
    '長崎県': '420010',
    '熊本県': '430010',
    '大分県': '440010',
    '宮崎県': '450010',
    '鹿児島県': '460010',
    '沖縄県': '471010'
}

def download_and_extract_zip(url, csv_name):
    """
    郵便番号データをダウンロードし、CSVの内容をリストとして返す
    """
    print(f"Downloading {url}...")
    response = requests.get(url)
    response.raise_for_status()

    print(f"Extracting {csv_name} from in-memory zip file...")
    with zipfile.ZipFile(io.BytesIO(response.content)) as zf:
        with zf.open(csv_name) as csv_file:
            # Decode the file and split into lines for the csv reader
            return io.TextIOWrapper(csv_file, 'utf-8').read().splitlines()

def create_prefix_to_prefecture_map(csv_lines):
    """
    郵便番号CSVの行リストから、上位3桁 -> 都道府県名のマッピングを作成
    """
    print("Creating postal code prefix to prefecture map...")
    prefix_map = {}
    
    # The csv module handles lines
    csv_reader = csv.reader(csv_lines)
    
    for row in csv_reader:
        # CSVの3列目: 郵便番号(7桁), 7列目: 都道府県名
        if len(row) > 6:
            postal_code = row[2]
            prefecture = row[6]
            
            if len(postal_code) >= 3:
                prefix = postal_code[:3]
                if prefix not in prefix_map:
                    prefix_map[prefix] = prefecture
            
    return prefix_map

def generate_final_mapping(prefix_to_prefecture, prefecture_to_area):
    """
    2つのマッピングを結合して、最終的な郵便番号3桁 -> 地域コードのマッピングを生成
    """
    print("Generating final mapping...")
    final_map = {}
    for prefix, prefecture in prefix_to_prefecture.items():
        area_code = prefecture_to_area.get(prefecture)
        if area_code:
            final_map[prefix] = area_code
        else:
            print(f"Warning: No area code found for prefecture: {prefecture} (prefix: {prefix})")
            
    return final_map

def main():
    """
    メイン処理
    """
    try:
        # 1. データのダウンロードと展開
        csv_lines = download_and_extract_zip(POSTAL_CODE_URL, CSV_FILE_NAME)
        
        # 2. 郵便番号プレフィックス -> 都道府県 のマッピング作成
        prefix_to_prefecture_map = create_prefix_to_prefecture_map(csv_lines)
        
        # 3. 最終的なマッピングの生成
        final_mapping = generate_final_mapping(prefix_to_prefecture_map, PREFECTURE_TO_AREA_CODE)
        
        # 4. JSONファイルに出力
        output_path = os.path.join(OUTPUT_DIR, OUTPUT_FILE_NAME)
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        
        print(f"Writing final mapping to {output_path}...")
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(final_mapping, f, ensure_ascii=False, indent=2, sort_keys=True)
            
        print("Successfully generated postal_code_mapping.json!")
        print(f"Total {len(final_mapping)} prefixes mapped.")
        
    except requests.exceptions.RequestException as e:
        print(f"Error: Failed to download postal code data. {e}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")


if __name__ == "__main__":
    main()
