import os

def rename_content():
    for root, dirs, files in os.walk('.github'):
        for file in files:
            filepath = os.path.join(root, file)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                new_content = content.replace('ArchiveTune', 'PaperTunes')
                new_content = new_content.replace('archivetune', 'papertunes')
                new_content = new_content.replace('ARCHIVETUNE', 'PAPERTUNES')
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated file: {filepath}")
            except Exception as e:
                pass

if __name__ == '__main__':
    rename_content()
