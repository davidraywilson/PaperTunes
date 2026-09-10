import os

def check_files():
    found = False
    for root, dirs, files in os.walk('.'):
        if '.git' in root or 'build' in root or '.gradle' in root or '.idea' in root:
            continue
        for file in files:
            if file.endswith(('.png', '.jpg', '.jpeg', '.webp', '.jar', '.jks', '.pyc', '.DS_Store', '.zip', '.svg', '.ttf', '.otf', '.ico', '.apk', '.py')):
                continue
            filepath = os.path.join(root, file)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                if 'archivetune' in content.lower():
                    print(f"Found in: {filepath}")
                    found = True
            except Exception as e:
                pass
    if not found:
        print("No remaining references found.")

if __name__ == '__main__':
    check_files()
