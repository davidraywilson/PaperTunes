import os

def rename_content():
    for root, dirs, files in os.walk('.'):
        if '.git' in root or 'build' in root or '.gradle' in root or '.idea' in root:
            continue
        for file in files:
            if file.endswith(('.png', '.jpg', '.jpeg', '.webp', '.jar', '.jks', '.pyc', '.DS_Store', '.zip', '.svg', '.ttf', '.otf', '.ico', '.apk')):
                continue
            filepath = os.path.join(root, file)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                new_content = content.replace('paperTunes', 'paperTunes')
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated file: {filepath}")
            except Exception as e:
                pass

if __name__ == '__main__':
    rename_content()
