import os

def rename_content():
    for root, dirs, files in os.walk('.'):
        if '.git' in root or 'build' in root or '.gradle' in root or '.idea' in root:
            continue
        for file in files:
            if file.endswith(('.png', '.jpg', '.jpeg', '.webp', '.jar', '.jks', '.pyc', '.DS_Store', '.zip', '.svg', '.ttf', '.otf', '.ico')):
                continue
            filepath = os.path.join(root, file)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                new_content = content.replace('PaperTunes', 'PaperTunes')
                new_content = new_content.replace('papertunes', 'papertunes')
                new_content = new_content.replace('PAPERTUNES', 'PAPERTUNES')
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated file: {filepath}")
            except Exception as e:
                # might be binary file that slipped through
                pass

def rename_dirs():
    for root, dirs, files in os.walk('.', topdown=False):
        if '.git' in root or 'build' in root or '.gradle' in root or '.idea' in root:
            continue
        for dir_name in dirs:
            if dir_name == 'papertunes':
                old_path = os.path.join(root, dir_name)
                new_path = os.path.join(root, 'papertunes')
                os.rename(old_path, new_path)
                print(f"Renamed directory: {old_path} -> {new_path}")

if __name__ == '__main__':
    rename_content()
    rename_dirs()
