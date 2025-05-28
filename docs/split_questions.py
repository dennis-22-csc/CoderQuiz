import os
import sys
from bs4 import BeautifulSoup

def split_questions(input_file, output_dir="output_questions"):
    # Ensure output directory exists
    os.makedirs(output_dir, exist_ok=True)

    # Read the input HTML file
    with open(input_file, "r", encoding="utf-8") as f:
        soup = BeautifulSoup(f, "html.parser")

    # Extract head content for reuse
    head = soup.head
    h1_tag = soup.find("h1")

    # Extract all question divs
    questions = soup.find_all("div", class_="question")

    for question in questions:
        q_id = question.get("id", "unknown_id")
        filename = f"{q_id}.html"
        output_path = os.path.join(output_dir, filename)

        # Build full HTML document
        new_html = BeautifulSoup("<!DOCTYPE html><html><head></head><body></body></html>", "html.parser")

        # Copy head
        new_html.head.replace_with(head)

        # Create a fresh <h1> tag to avoid reusing the same element
        if h1_tag:
            new_h1 = new_html.new_tag("h1")
            new_h1.string = h1_tag.get_text()
            new_html.body.append(new_h1)

        # Copy the question div into the new HTML
        new_question = question.extract()
        new_html.body.append(new_question)

        # Write to new file
        with open(output_path, "w", encoding="utf-8") as out_file:
            out_file.write(str(new_html.prettify()))

        print(f"Created: {output_path}")

if __name__ == "__main__":
    input_html = "index.html"
    output_path="Large Language Models: A Survey by Shervin Minaee, Tomas Mikolov, Narjes Nikzad, Meysam Chenaghlu Richard Socher, Xavier Amatriain, Jianfeng Gao"
    split_questions(input_html, output_path)

