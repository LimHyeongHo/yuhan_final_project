import hashlib

test_string = ["","","","",""]
hex_dig_arr = ["","","","",""]
previous_hash = "00000000000000000000000000000000"

for i in range(0, 5):
    test_string[i] = input(f"{i+1}번째 입력: ")
    combined = previous_hash + test_string[i]
    hash_object = hashlib.sha256(combined.encode()) 
    hex_dig = hash_object.hexdigest()

    ## print(f"해시 데이터 {i+1}번:", hex_dig) 
    hex_dig_arr[i] = hex_dig  
    previous_hash = hex_dig

for i in range(0,5):
    print(f"해시 데이터 {i+1}번:", hex_dig_arr[i])

while True:
    input_num = int(input("몇 번째 해시 데이터를 수정하고 싶으신가요? (1~5) 0 입력시 종료: "))
    if input_num == 0:
        print("프로그램을 종료합니다.")
        break
    input_num -= 1
    if 0 <= input_num <= 4:
        new_string = input("새로운 문자열을 입력하세요: ")

        # 2. 수정하려는 블록의 '진짜 이전 해시'를 찾아오는 로직 추가
        if input_num == 0:
            current_prev_hash = "00000000000000000000000000000000" # 1번 블록일 경우
        else:
            current_prev_hash = hex_dig_arr[input_num - 1] # 그 외의 경우 바로 앞 블록의 해시

        combined = current_prev_hash + new_string
        hash_object = hashlib.sha256(combined.encode()) 
        new_hex_dig = hash_object.hexdigest()

        print(f"저장된 해시 데이터 {input_num+1}번:", hex_dig_arr[input_num])
        print(f"수정된 해시 데이터 {input_num+1}번:", new_hex_dig)

        if hex_dig_arr[input_num] != new_hex_dig:
            print(f"해시 데이터 {input_num+1}번이 변경되었습니다.")
        
    else :
        print("잘못된 입력입니다. 1~5 사이의 숫자를 입력해주세요.")